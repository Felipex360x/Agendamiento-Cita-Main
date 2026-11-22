package cl.nicolet.backend.service;

import cl.nicolet.backend.dto.PagoHistorialDTO;
import cl.nicolet.backend.dto.PagoHistorialRespuestaDTO;
import cl.nicolet.backend.dto.PagoInicioDTO;
import cl.nicolet.backend.exception.ConflictoException;
import cl.nicolet.backend.exception.RecursoNoEncontradoException;
import cl.nicolet.backend.model.Cita;
import cl.nicolet.backend.model.EstadoCita;
import cl.nicolet.backend.model.EstadoPago;
import cl.nicolet.backend.model.EstadoTransaccionPago;
import cl.nicolet.backend.model.Pago;
import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.payment.WebpayException;
import cl.nicolet.backend.payment.WebpayPlusClient;
import cl.nicolet.backend.repository.CitaRepository;
import cl.nicolet.backend.repository.PagoRepository;
import cl.nicolet.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Pago de citas con Webpay Plus.
 *
 * <p>Reglas:
 * <ul>
 *   <li>Al reservar con pago en línea el horario queda bloqueado (cita PENDIENTE + PENDIENTE_PAGO)
 *       hasta que el pago se complete o venza el plazo.</li>
 *   <li>Mientras no esté pagada no se avisa al profesional.</li>
 *   <li>Si el pago se aprueba se envían, en este orden: comprobante al cliente, aviso a administración
 *       y aviso al profesional.</li>
 *   <li>Si el pago se rechaza, se anula o vence, la cita se cancela y el horario queda libre.</li>
 *   <li>No hay devoluciones: el pago solo se confirma con Webpay cuando el cliente vuelve del formulario
 *       y la cita sigue esperando el pago.</li>
 * </ul>
 * Los cambios en una cita se hacen con la fila de la cita bloqueada, así que confirmar un pago y
 * vencer el plazo nunca ocurren a la vez.
 */
@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    public enum ResultadoRetorno { APROBADO, RECHAZADO, ANULADO, EXPIRADO, ERROR }

    /** Resultado que se le muestra al cliente al volver de Webpay. {@code codigoReserva} puede ser null. */
    public record RetornoPago(ResultadoRetorno resultado, String codigoReserva) {
    }

    private record Procesado(RetornoPago retorno, Cita citaPagada, Pago pagoAprobado, List<String> correosAdmin,
                             Cita citaRechazada, Pago pagoRechazado) {
        static Procesado sinAvisos(ResultadoRetorno r, String codigo) {
            return new Procesado(new RetornoPago(r, codigo), null, null, List.of(), null, null);
        }

        static Procesado aprobado(String codigo, Cita cita, Pago pago, List<String> correosAdmin) {
            return new Procesado(new RetornoPago(ResultadoRetorno.APROBADO, codigo), cita, pago, correosAdmin, null, null);
        }

        static Procesado rechazado(String codigo, Cita cita, Pago pago) {
            return new Procesado(new RetornoPago(ResultadoRetorno.RECHAZADO, codigo), null, null, List.of(), cita, pago);
        }
    }

    private final Clock clock;
    private final CitaRepository citaRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final WebpayPlusClient webpay;
    private final TransactionTemplate tx;

    @Autowired(required = false)
    private EmailService emailService;

    @Value("${webpay.return-url}")
    private String returnUrl;

    @Value("${app.admin-notification-email:}")
    private String correosAdminConfigurados = "";

    @Value("${app.admin.email:}")
    private String correoAdminPrincipal = "";

    public PagoService(Clock clock, CitaRepository citaRepository, PagoRepository pagoRepository,
                       UsuarioRepository usuarioRepository, WebpayPlusClient webpay,
                       PlatformTransactionManager transactionManager) {
        this.clock = clock;
        this.citaRepository = citaRepository;
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.webpay = webpay;
        this.tx = new TransactionTemplate(transactionManager);
        // Lectura actualizada: después de bloquear la cita se necesita ver lo último que se guardó.
        this.tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    /** Crea la transacción en Webpay para una cita que espera pago. */
    public PagoInicioDTO iniciarPago(Long citaId) {
        return tx.execute(status -> {
            Cita cita = citaRepository.findByIdForUpdate(citaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada: " + citaId));
            if (cita.getEstadoPago() != EstadoPago.PENDIENTE_PAGO || cita.getEstado() != EstadoCita.PENDIENTE) {
                throw new ConflictoException("Esta cita no está esperando un pago.");
            }
            if (plazoVencido(cita)) {
                throw new ConflictoException("El plazo para pagar esta cita venció. Vuelve a reservar tu hora.");
            }
            long monto = montoEnPesos(cita);

            // Si el cliente ya abrió el pago (por ejemplo, tocó el botón dos veces) se reutiliza.
            for (Pago existente : pagoRepository.findByCitaIdAndEstado(citaId, EstadoTransaccionPago.INICIADO)) {
                if (existente.getToken() != null && existente.getUrlPago() != null) {
                    return new PagoInicioDTO(citaId, cita.getCodigoReserva(), existente.getToken(),
                            existente.getUrlPago(), existente.getMonto());
                }
            }

            Pago pago = new Pago();
            pago.setCita(cita);
            // Código de reserva (15 caracteres) + intento: cabe en los 26 que admite Webpay y no se repite.
            pago.setBuyOrder(cita.getCodigoReserva() + "-" + (pagoRepository.countByCitaId(citaId) + 1));
            pago.setSessionId(UUID.randomUUID().toString());
            pago.setMonto(monto);
            pago.setCreadoEn(LocalDateTime.now(clock));

            WebpayPlusClient.CrearRespuesta respuesta =
                    webpay.crear(pago.getBuyOrder(), pago.getSessionId(), monto, returnUrl);
            pago.setToken(respuesta.token());
            pago.setUrlPago(respuesta.url());
            pagoRepository.save(pago);
            log.info("Pago iniciado: cita={} buyOrder={} monto={}", cita.getCodigoReserva(), pago.getBuyOrder(), monto);
            return new PagoInicioDTO(citaId, cita.getCodigoReserva(), pago.getToken(), pago.getUrlPago(), monto);
        });
    }

    /**
     * Procesa el regreso del cliente desde Webpay.
     * <ul>
     *   <li>Solo {@code token_ws}: el cliente pagó (o intentó pagar) y hay que confirmar con Webpay.</li>
     *   <li>{@code TBK_TOKEN} presente: el cliente anuló el pago en el formulario.</li>
     *   <li>Solo {@code TBK_ORDEN_COMPRA} y {@code TBK_ID_SESION}: venció el tiempo del formulario.</li>
     * </ul>
     */
    public RetornoPago procesarRetorno(String tokenWs, String tbkToken, String ordenCompra, String idSesion) {
        Long citaId = localizarCita(tokenWs, tbkToken, ordenCompra);
        if (citaId == null) {
            log.warn("Retorno de Webpay que no corresponde a ningún pago conocido");
            return new RetornoPago(ResultadoRetorno.ERROR, null);
        }
        Procesado procesado = tx.execute(status -> procesarEnTransaccion(citaId, tokenWs, tbkToken, ordenCompra, idSesion));
        if (procesado == null) {
            return new RetornoPago(ResultadoRetorno.ERROR, null);
        }
        if (procesado.citaPagada() != null) {
            notificarPagoAprobado(procesado.citaPagada(), procesado.pagoAprobado(), procesado.correosAdmin());
        }
        if (procesado.citaRechazada() != null) {
            notificarPagoRechazado(procesado.citaRechazada(), procesado.pagoRechazado());
        }
        return procesado.retorno();
    }

    /** Cancela las citas que no se pagaron a tiempo y deja libre su horario. */
    public int expirarPagosVencidos() {
        List<Long> ids = citaRepository.findIdsConPagoVencido(LocalDateTime.now(clock));
        int expiradas = 0;
        for (Long id : ids) {
            try {
                Integer resultado = tx.execute(status -> {
                    Cita cita = citaRepository.findByIdForUpdate(id).orElse(null);
                    if (cita == null || cita.getEstadoPago() != EstadoPago.PENDIENTE_PAGO
                            || cita.getEstado() != EstadoCita.PENDIENTE || !plazoVencido(cita)) {
                        return 0;
                    }
                    marcarIntentosIniciados(cita.getId(), EstadoTransaccionPago.EXPIRADO);
                    cancelarSinPago(cita, "Venció el plazo para pagar la reserva");
                    return 1;
                });
                expiradas += resultado == null ? 0 : resultado;
            } catch (RuntimeException e) {
                log.warn("No se pudo expirar la cita {} sin pago: {}", id, e.getMessage());
            }
        }
        if (expiradas > 0) {
            log.info("Citas sin pago canceladas por vencimiento: {}", expiradas);
        }
        return expiradas;
    }

    /** Máximo de pagos que devuelve el historial en una consulta. */
    static final int LIMITE_HISTORIAL = 1000;

    /**
     * Historial de pagos para administración, del más nuevo al más antiguo.
     * {@code desde} y {@code hasta} (ambos incluidos) filtran por el día en que se inició el pago.
     */
    public PagoHistorialRespuestaDTO historial(EstadoTransaccionPago estado, LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'.");
        }
        LocalDateTime inicio = desde != null ? desde.atStartOfDay() : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime fin = hasta != null ? hasta.plusDays(1).atStartOfDay() : LocalDateTime.of(2999, 1, 1, 0, 0);
        List<Pago> pagos = pagoRepository.historial(inicio, fin, estado, PageRequest.of(0, LIMITE_HISTORIAL + 1));
        boolean truncado = pagos.size() > LIMITE_HISTORIAL;
        List<PagoHistorialDTO> filas = (truncado ? pagos.subList(0, LIMITE_HISTORIAL) : pagos).stream()
                .map(PagoService::aFila).toList();
        return new PagoHistorialRespuestaDTO(filas, truncado);
    }

    private static PagoHistorialDTO aFila(Pago p) {
        Cita c = p.getCita();
        Usuario cliente = c.getCliente().getUsuario();
        Usuario pro = c.getTrabajador().getUsuario();
        boolean aprobado = p.getEstado() == EstadoTransaccionPago.APROBADO;
        return new PagoHistorialDTO(p.getId(), c.getId(), c.getCodigoReserva(), p.getEstado().name(), c.getEstado().name(),
                p.getMonto(), nombre(cliente), cliente.getCorreo(), c.getServicio().getNombre(), nombre(pro),
                c.getFechaHoraInicio(), p.getBuyOrder(),
                // Solo un pago aprobado tiene datos de tarjeta y autorización que mostrar.
                aprobado ? p.getAuthorizationCode() : null,
                aprobado ? EmailService.descripcionTipoPago(p.getPaymentTypeCode(), p.getInstallmentsNumber()) : null,
                aprobado ? p.getCardLast4() : null, aprobado ? p.getTransactionDate() : null,
                p.getResponseCode(), p.getCreadoEn(), p.getActualizadoEn());
    }

    private static String nombre(Usuario u) {
        return ((u.getNombre() == null ? "" : u.getNombre()) + " " + (u.getApellidoP() == null ? "" : u.getApellidoP())).trim();
    }

    // Interior

    private Procesado procesarEnTransaccion(Long citaId, String tokenWs, String tbkToken, String ordenCompra, String idSesion) {
        // Primero se bloquea la cita; todo lo demás se lee después para ver el estado más reciente.
        Cita cita = citaRepository.findByIdForUpdate(citaId).orElse(null);
        if (cita == null) {
            return Procesado.sinAvisos(ResultadoRetorno.ERROR, null);
        }
        String codigo = cita.getCodigoReserva();
        boolean anulo = StringUtils.hasText(tbkToken);
        boolean confirmar = StringUtils.hasText(tokenWs) && !anulo;

        Pago pago = anulo ? pagoRepository.findByToken(tbkToken).orElse(null)
                : confirmar ? pagoRepository.findByToken(tokenWs).orElse(null)
                : pagoRepository.findByBuyOrder(ordenCompra).orElse(null);
        if (pago == null || !pago.getCita().getId().equals(citaId)) {
            return Procesado.sinAvisos(ResultadoRetorno.ERROR, null);
        }
        // Sin token, el único dato de confianza es el session_id aleatorio que Webpay devuelve.
        if (!anulo && !confirmar && !pago.getSessionId().equals(idSesion)) {
            log.warn("Aviso de vencimiento con session_id que no coincide (orden {})", pago.getBuyOrder());
            return Procesado.sinAvisos(ResultadoRetorno.ERROR, null);
        }

        // Si el cliente refresca la página, el pago ya quedó resuelto: se informa lo mismo sin repetir nada.
        if (pago.getEstado() != EstadoTransaccionPago.INICIADO) {
            return Procesado.sinAvisos(resultadoDe(pago.getEstado()), codigo);
        }

        if (!confirmar) {
            pago.setEstado(anulo ? EstadoTransaccionPago.ANULADO_POR_USUARIO : EstadoTransaccionPago.EXPIRADO);
            tocar(pago);
            cancelarSinPago(cita, anulo ? "El cliente anuló el pago" : "Venció el tiempo del formulario de pago");
            return Procesado.sinAvisos(anulo ? ResultadoRetorno.ANULADO : ResultadoRetorno.EXPIRADO, codigo);
        }

        // Si la cita ya no espera pago (por ejemplo, venció el plazo) NO se confirma con Webpay:
        // al no confirmarse, Webpay no cobra la transacción.
        if (cita.getEstadoPago() != EstadoPago.PENDIENTE_PAGO || cita.getEstado() != EstadoCita.PENDIENTE
                || plazoVencido(cita)) {
            pago.setEstado(EstadoTransaccionPago.EXPIRADO);
            tocar(pago);
            if (cita.getEstado() == EstadoCita.PENDIENTE && cita.getEstadoPago() == EstadoPago.PENDIENTE_PAGO) {
                cancelarSinPago(cita, "Venció el plazo para pagar la reserva");
            }
            return Procesado.sinAvisos(ResultadoRetorno.EXPIRADO, codigo);
        }

        WebpayPlusClient.Resultado resultado = confirmarOConsultar(tokenWs);
        if (resultado == null) {
            pago.setEstado(EstadoTransaccionPago.ERROR);
            tocar(pago);
            cancelarSinPago(cita, "No se pudo confirmar el pago con Webpay");
            return Procesado.sinAvisos(ResultadoRetorno.ERROR, codigo);
        }

        pago.setResponseCode(resultado.responseCode());
        pago.setAuthorizationCode(resultado.authorizationCode());
        pago.setPaymentTypeCode(resultado.paymentTypeCode());
        pago.setInstallmentsNumber(resultado.installmentsNumber());
        pago.setTransactionDate(resultado.transactionDate());
        pago.setCardLast4(resultado.cardDetail() != null ? resultado.cardDetail().cardNumber() : null);

        if (resultado.aprobada()) {
            boolean coincide = pago.getBuyOrder().equals(resultado.buyOrder())
                    && resultado.amount() != null && resultado.amount().compareTo(BigDecimal.valueOf(pago.getMonto())) == 0;
            if (!coincide) {
                log.error("Webpay aprobó un pago que no coincide con la orden {} (monto o orden distintos)", pago.getBuyOrder());
                pago.setEstado(EstadoTransaccionPago.ERROR);
                tocar(pago);
                cancelarSinPago(cita, "Los datos del pago no coinciden con la reserva");
                return Procesado.sinAvisos(ResultadoRetorno.ERROR, codigo);
            }
            pago.setEstado(EstadoTransaccionPago.APROBADO);
            tocar(pago);
            cita.setEstadoPago(EstadoPago.PAGADO);
            cita.setPagoExpiraEn(null);
            citaRepository.save(cita);
            log.info("Pago aprobado: cita={} buyOrder={}", codigo, pago.getBuyOrder());
            return Procesado.aprobado(codigo, cita, pago, destinatariosAdministracion());
        }

        pago.setEstado(EstadoTransaccionPago.RECHAZADO);
        tocar(pago);
        cancelarSinPago(cita, "El pago fue rechazado");
        log.info("Pago rechazado: cita={} buyOrder={} responseCode={}", codigo, pago.getBuyOrder(), resultado.responseCode());
        return Procesado.rechazado(codigo, cita, pago);
    }

    /** Confirma el pago; si no se supo el resultado, lo consulta. Devuelve null si no hay forma de saberlo. */
    private WebpayPlusClient.Resultado confirmarOConsultar(String token) {
        try {
            return webpay.confirmar(token);
        } catch (WebpayException e) {
            log.warn("No se pudo confirmar el pago con Webpay ({}); se consulta su estado", e.getMessage());
        }
        try {
            WebpayPlusClient.Resultado consultado = webpay.consultar(token);
            // Una transacción aún sin confirmar no está pagada.
            return "INITIALIZED".equals(consultado.status()) ? null : consultado;
        } catch (WebpayException e) {
            log.error("Tampoco se pudo consultar el estado del pago: {}", e.getMessage());
            return null;
        }
    }

    private Long localizarCita(String tokenWs, String tbkToken, String ordenCompra) {
        if (StringUtils.hasText(tbkToken)) {
            return pagoRepository.findCitaIdByToken(tbkToken).orElse(null);
        }
        if (StringUtils.hasText(tokenWs)) {
            return pagoRepository.findCitaIdByToken(tokenWs).orElse(null);
        }
        if (StringUtils.hasText(ordenCompra)) {
            return pagoRepository.findCitaIdByBuyOrder(ordenCompra).orElse(null);
        }
        return null;
    }

    private void cancelarSinPago(Cita cita, String motivo) {
        if (cita.getEstado() == EstadoCita.PENDIENTE) {
            cita.setEstado(EstadoCita.CANCELADA);
            cita.setMotivoCancelacion(motivo);
            cita.setPagoExpiraEn(null);
            citaRepository.save(cita);
            log.info("Cita {} cancelada por falta de pago: {}", cita.getCodigoReserva(), motivo);
        }
    }

    private void marcarIntentosIniciados(Long citaId, EstadoTransaccionPago nuevoEstado) {
        for (Pago intento : pagoRepository.findByCitaIdAndEstado(citaId, EstadoTransaccionPago.INICIADO)) {
            intento.setEstado(nuevoEstado);
            tocar(intento);
        }
    }

    private void tocar(Pago pago) {
        pago.setActualizadoEn(LocalDateTime.now(clock));
        pagoRepository.save(pago);
    }

    private boolean plazoVencido(Cita cita) {
        return cita.getPagoExpiraEn() != null && !LocalDateTime.now(clock).isBefore(cita.getPagoExpiraEn());
    }

    private static ResultadoRetorno resultadoDe(EstadoTransaccionPago estado) {
        return switch (estado) {
            case APROBADO -> ResultadoRetorno.APROBADO;
            case RECHAZADO -> ResultadoRetorno.RECHAZADO;
            case ANULADO_POR_USUARIO -> ResultadoRetorno.ANULADO;
            case EXPIRADO -> ResultadoRetorno.EXPIRADO;
            case ERROR, INICIADO -> ResultadoRetorno.ERROR;
        };
    }

    /** Webpay solo acepta montos enteros en pesos. */
    static long montoEnPesos(Cita cita) {
        BigDecimal precio = cita.getPrecioFinal();
        if (precio == null || precio.signum() <= 0) {
            throw new IllegalArgumentException("La cita no tiene un monto válido para cobrar");
        }
        try {
            return precio.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("El precio del servicio debe ser un monto entero en pesos para pagar con Webpay");
        }
    }

    /**
     * Cuenta principal de administración, la única que recibe el aviso de cita pagada. Se toma, en este orden,
     * de {@code ADMIN_NOTIFICATION_EMAIL}, del correo del primer administrador ({@code APP_ADMIN_EMAIL}) o,
     * si no hay ninguno configurado, del administrador activo más antiguo.
     */
    List<String> destinatariosAdministracion() {
        Set<String> correos = new LinkedHashSet<>();
        if (correosAdminConfigurados != null) {
            Arrays.stream(correosAdminConfigurados.split("[,;]"))
                    .map(String::trim).filter(c -> !c.isEmpty()).forEach(correos::add);
        }
        if (correos.isEmpty() && StringUtils.hasText(correoAdminPrincipal)) {
            correos.add(correoAdminPrincipal.trim());
        }
        if (correos.isEmpty()) {
            usuarioRepository.findByTipoUsuarioNombreIgnoreCase("ADMINISTRADOR").stream()
                    .filter(admin -> admin.getEstado() == Usuario.ESTADO_ACTIVO && StringUtils.hasText(admin.getCorreo()))
                    .min(Comparator.comparing(Usuario::getId))
                    .ifPresent(admin -> correos.add(admin.getCorreo().trim()));
        }
        return new ArrayList<>(correos);
    }

    /**
     * Avisos posteriores al pago, en este orden: comprobante al cliente, aviso a administración y,
     * recién después, aviso al profesional. Un fallo de correo no impide los siguientes ni el pago.
     */
    private void notificarPagoAprobado(Cita cita, Pago pago, List<String> correosAdmin) {
        if (emailService == null) {
            return;
        }
        enviarSeguro("comprobante al cliente", cita, () -> emailService.enviarComprobantePagoYReserva(cita, pago));
        enviarSeguro("aviso a administración", cita, () -> emailService.enviarAvisoPagoAdministracion(cita, pago, correosAdmin));
        enviarSeguro("aviso al profesional", cita, () -> emailService.enviarAvisoNuevaCitaTrabajador(cita));
    }

    /** Al cliente se le avisa que el pago no se hizo y que su hora quedó libre. Un fallo de correo no afecta al resto. */
    private void notificarPagoRechazado(Cita cita, Pago pago) {
        if (emailService == null) {
            return;
        }
        enviarSeguro("aviso de pago rechazado al cliente", cita, () -> emailService.enviarPagoRechazado(cita, pago));
    }

    private void enviarSeguro(String descripcion, Cita cita, Runnable envio) {
        try {
            envio.run();
        } catch (Exception e) {
            log.warn("Error no bloqueante al enviar {} de la cita {}: {}", descripcion, cita.getCodigoReserva(), e.getMessage());
        }
    }
}
