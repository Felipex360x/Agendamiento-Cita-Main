package cl.nicolet.backend.service;

import cl.nicolet.backend.dto.CorreoEnviadoDTO;
import cl.nicolet.backend.model.Cita;
import cl.nicolet.backend.model.Servicio;
import cl.nicolet.backend.model.Trabajador;
import cl.nicolet.backend.model.Usuario;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA_HORA_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private static final int MAX_BANDEJA = 200;

    private final AtomicLong secuenciaId = new AtomicLong(1);
    private final CopyOnWriteArrayList<CorreoEnviadoDTO> bandejaEnviados = new CopyOnWriteArrayList<>();

    @Autowired(required = false)
    private Clock clock = Clock.systemDefaultZone();

    @Value("${spring.mail.host:smtp.gmail.com}")
    private volatile String mailHost = "smtp.gmail.com";

    @Value("${spring.mail.port:587}")
    private volatile int mailPort = 587;

    @Value("${spring.mail.username:nacuna888@gmail.com}")
    private volatile String mailUsername = "nacuna888@gmail.com";

    @Value("${spring.mail.password:}")
    private volatile String mailPassword = "";

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl = "http://localhost:5173";

    @jakarta.annotation.PostConstruct
    public void inicializar() {
        if (this.mailPassword != null) {
            this.mailPassword = this.mailPassword.replace(" ", "").trim();
        }
        log.info("EmailService inicializado: host={}, port={}, username={}, smtpConfigurado={}",
                this.mailHost, this.mailPort, this.mailUsername, isSmtpConfigurado());
    }

    /**
     * Cambia solo el remitente y su contraseña de aplicación. El servidor y el puerto SMTP
     * se definen únicamente por configuración y no se pueden modificar en ejecución.
     */
    public synchronized void configurarCredencialesSmtp(String username, String password) {
        if (username != null && !username.isBlank()) {
            this.mailUsername = username.trim();
        }
        if (password != null) {
            // Las contraseñas de aplicación de Google suelen copiarse con espacios cada 4 letras
            this.mailPassword = password.replace(" ", "").trim();
        }
        log.info("Credenciales SMTP actualizadas: host={}, port={}, passwordConfigurada={}",
                this.mailHost, this.mailPort, isSmtpConfigurado());
    }

    public boolean isSmtpConfigurado() {
        String passLimpia = mailPassword != null ? mailPassword.replace(" ", "").trim() : "";
        return !passLimpia.isBlank() && mailUsername != null && !mailUsername.isBlank();
    }

    public String getMailUsername() {
        return mailUsername;
    }

    public String getMailHost() {
        return mailHost;
    }

    public int getMailPort() {
        return mailPort;
    }

    public List<CorreoEnviadoDTO> listarCorreosEnviados() {
        return new ArrayList<>(bandejaEnviados);
    }

    public void limpiarBandeja() {
        bandejaEnviados.clear();
    }

    /**
     * 1. Correo de Bienvenida al crear cuenta de cliente.
     */
    public CorreoEnviadoDTO enviarBienvenida(Usuario usuario) {
        if (usuario == null || usuario.getCorreo() == null) return null;
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("nombreCliente", valor(usuario.getNombre(), "Clienta"));
        vars.put("apellidoCliente", valor(usuario.getApellidoP(), ""));
        vars.put("correoCliente", usuario.getCorreo());
        vars.put("urlReservar", frontendBaseUrl + "/reservar");

        String asunto = "¡Bienvenida a Nicolett Studio Fantasy, " + vars.get("nombreCliente") + "!";
        String html = renderizarPlantilla("bienvenida.html", vars);
        String nombreCompleto = (vars.get("nombreCliente") + " " + vars.get("apellidoCliente")).trim();
        return despacharCorreo("BIENVENIDA", usuario.getCorreo(), nombreCompleto, asunto, null, html);
    }

    /**
     * 2. Correo de Comprobante de Pago y Solicitud de Reserva al cliente.
     */
    public CorreoEnviadoDTO enviarComprobantePagoYReserva(Cita cita) {
        if (cita == null || cita.getCliente() == null || cita.getCliente().getUsuario() == null) return null;
        Usuario uCliente = cita.getCliente().getUsuario();
        Usuario uTrabajador = cita.getTrabajador() != null ? cita.getTrabajador().getUsuario() : null;
        Servicio servicio = cita.getServicio();

        LocalDateTime ahora = LocalDateTime.now(clock != null ? clock : Clock.systemDefaultZone());
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("nombreCliente", valor(uCliente.getNombre(), "Clienta"));
        vars.put("apellidoCliente", valor(uCliente.getApellidoP(), ""));
        vars.put("codigoReserva", valor(cita.getCodigoReserva(), "RES-2026-000000"));
        vars.put("nombreServicio", servicio != null ? valor(servicio.getNombre(), "Servicio") : "Servicio");
        vars.put("duracionMinutos", servicio != null && servicio.getDuracionMinutos() != null
                ? String.valueOf(servicio.getDuracionMinutos()) : "60");
        vars.put("nombreTrabajador", nombreCompleto(uTrabajador, "Especialista"));
        vars.put("fechaCita", cita.getFechaHoraInicio() != null ? cita.getFechaHoraInicio().format(FECHA_FMT) : ahora.format(FECHA_FMT));
        vars.put("horaCita", cita.getFechaHoraInicio() != null ? cita.getFechaHoraInicio().format(HORA_FMT) : "10:00");
        vars.put("ordenCompra", "OC-" + vars.get("codigoReserva").replace("RES-", ""));
        vars.put("codigoAutorizacion", "1213");
        vars.put("tipoPago", "Venta Débito (VD)");
        vars.put("ultimos4Digitos", "6623");
        vars.put("fechaTransaccion", ahora.format(FECHA_HORA_FMT));
        vars.put("montoPagado", formatearMoneda(cita.getPrecioFinal() != null
                ? cita.getPrecioFinal()
                : (servicio != null ? servicio.getPrecio() : BigDecimal.ZERO)));
        vars.put("urlMiPerfil", frontendBaseUrl + "/mi-perfil");

        String asunto = "Comprobante de pago y solicitud de reserva " + vars.get("codigoReserva") + " | Nicolett Studio Fantasy";
        String html = renderizarPlantilla("comprobante-pago.html", vars);
        return despacharCorreo("PAGO_RESERVA", uCliente.getCorreo(),
                nombreCompleto(uCliente, "Clienta"), asunto, vars.get("codigoReserva"), html);
    }

    /**
     * 3. Correo al Trabajador (Especialista) cuando agendan una cita con él/ella.
     */
    public CorreoEnviadoDTO enviarAvisoNuevaCitaTrabajador(Cita cita) {
        if (cita == null || cita.getTrabajador() == null || cita.getTrabajador().getUsuario() == null) return null;
        Usuario uTrabajador = cita.getTrabajador().getUsuario();
        Usuario uCliente = cita.getCliente() != null ? cita.getCliente().getUsuario() : null;
        Servicio servicio = cita.getServicio();

        LocalDateTime ahora = LocalDateTime.now(clock != null ? clock : Clock.systemDefaultZone());
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("nombreTrabajador", nombreCompleto(uTrabajador, "Especialista"));
        vars.put("nombreCliente", uCliente != null ? valor(uCliente.getNombre(), "Clienta") : "Clienta");
        vars.put("apellidoCliente", uCliente != null ? valor(uCliente.getApellidoP(), "") : "");
        vars.put("codigoReserva", valor(cita.getCodigoReserva(), "RES-2026-000000"));
        vars.put("nombreServicio", servicio != null ? valor(servicio.getNombre(), "Servicio") : "Servicio");
        vars.put("duracionMinutos", servicio != null && servicio.getDuracionMinutos() != null
                ? String.valueOf(servicio.getDuracionMinutos()) : "60");
        vars.put("fechaCita", cita.getFechaHoraInicio() != null ? cita.getFechaHoraInicio().format(FECHA_FMT) : ahora.format(FECHA_FMT));
        vars.put("horaInicio", cita.getFechaHoraInicio() != null ? cita.getFechaHoraInicio().format(HORA_FMT) : "10:00");
        vars.put("horaFin", cita.getFechaHoraFin() != null ? cita.getFechaHoraFin().format(HORA_FMT) : "11:00");
        vars.put("notasCliente", cita.getNotasCliente() != null && !cita.getNotasCliente().isBlank()
                ? cita.getNotasCliente() : "Sin observaciones adicionales.");
        vars.put("urlPanelTrabajador", frontendBaseUrl + "/admin/panel-trabajador");

        String asunto = "Nueva cita agendada contigo (" + vars.get("codigoReserva") + ") — "
                + (vars.get("nombreCliente") + " " + vars.get("apellidoCliente")).trim();
        String html = renderizarPlantilla("nueva-cita-trabajador.html", vars);
        return despacharCorreo("NUEVA_CITA_TRABAJADOR", uTrabajador.getCorreo(),
                vars.get("nombreTrabajador"), asunto, vars.get("codigoReserva"), html);
    }

    /**
     * 4. Correo al Cliente cuando el trabajador confirma su cita.
     */
    public CorreoEnviadoDTO enviarCitaConfirmada(Cita cita) {
        if (cita == null || cita.getCliente() == null || cita.getCliente().getUsuario() == null) return null;
        Usuario uCliente = cita.getCliente().getUsuario();
        Trabajador trabajador = cita.getTrabajador();
        Usuario uTrabajador = trabajador != null ? trabajador.getUsuario() : null;
        Servicio servicio = cita.getServicio();

        LocalDateTime ahora = LocalDateTime.now(clock != null ? clock : Clock.systemDefaultZone());
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("nombreCliente", valor(uCliente.getNombre(), "Clienta"));
        vars.put("codigoReserva", valor(cita.getCodigoReserva(), "RES-2026-000000"));
        vars.put("nombreServicio", servicio != null ? valor(servicio.getNombre(), "Servicio") : "Servicio");
        vars.put("duracionMinutos", servicio != null && servicio.getDuracionMinutos() != null
                ? String.valueOf(servicio.getDuracionMinutos()) : "60");
        vars.put("nombreTrabajador", nombreCompleto(uTrabajador, "Especialista"));
        vars.put("cargoEspecialidad", trabajador != null && trabajador.getCargoEspecialidad() != null
                ? trabajador.getCargoEspecialidad() : "Especialista");
        vars.put("fechaCita", cita.getFechaHoraInicio() != null ? cita.getFechaHoraInicio().format(FECHA_FMT) : ahora.format(FECHA_FMT));
        vars.put("horaCita", cita.getFechaHoraInicio() != null ? cita.getFechaHoraInicio().format(HORA_FMT) : "10:00");
        vars.put("urlMiPerfil", frontendBaseUrl + "/mi-perfil");

        String asunto = "¡Tu cita " + vars.get("codigoReserva") + " fue confirmada por " + vars.get("nombreTrabajador") + "!";
        String html = renderizarPlantilla("cita-confirmada.html", vars);
        return despacharCorreo("CITA_CONFIRMADA", uCliente.getCorreo(),
                nombreCompleto(uCliente, "Clienta"), asunto, vars.get("codigoReserva"), html);
    }

    /**
     * 5. Correo al Cliente cuando se cancela una cita con motivo.
     */
    public CorreoEnviadoDTO enviarCitaCancelada(Cita cita) {
        if (cita == null || cita.getCliente() == null || cita.getCliente().getUsuario() == null) return null;
        Usuario uCliente = cita.getCliente().getUsuario();
        Usuario uTrabajador = cita.getTrabajador() != null ? cita.getTrabajador().getUsuario() : null;
        Servicio servicio = cita.getServicio();

        LocalDateTime ahora = LocalDateTime.now(clock != null ? clock : Clock.systemDefaultZone());
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("nombreCliente", valor(uCliente.getNombre(), "Clienta"));
        vars.put("codigoReserva", valor(cita.getCodigoReserva(), "RES-2026-000000"));
        vars.put("nombreServicio", servicio != null ? valor(servicio.getNombre(), "Servicio") : "Servicio");
        vars.put("nombreTrabajador", nombreCompleto(uTrabajador, "Especialista"));
        vars.put("fechaCita", cita.getFechaHoraInicio() != null ? cita.getFechaHoraInicio().format(FECHA_FMT) : ahora.format(FECHA_FMT));
        vars.put("horaCita", cita.getFechaHoraInicio() != null ? cita.getFechaHoraInicio().format(HORA_FMT) : "10:00");
        vars.put("motivoCancelacion", valor(cita.getMotivoCancelacion(), "Cancelación informada por el estudio."));
        vars.put("urlReservar", frontendBaseUrl + "/reservar");

        String asunto = "Aviso de cancelación de tu reserva " + vars.get("codigoReserva") + " | Nicolett Studio Fantasy";
        String html = renderizarPlantilla("cita-cancelada.html", vars);
        return despacharCorreo("CITA_CANCELADA", uCliente.getCorreo(),
                nombreCompleto(uCliente, "Clienta"), asunto, vars.get("codigoReserva"), html);
    }

    /**
     * 5b. Correo al Trabajador informando la cancelación de una cita agendada.
     */
    public CorreoEnviadoDTO enviarAvisoCancelacionTrabajador(Cita cita) {
        if (cita == null || cita.getTrabajador() == null || cita.getTrabajador().getUsuario() == null) return null;
        Usuario uTrabajador = cita.getTrabajador().getUsuario();
        Usuario uCliente = cita.getCliente() != null ? cita.getCliente().getUsuario() : null;
        Servicio servicio = cita.getServicio();

        LocalDateTime ahora = LocalDateTime.now(clock != null ? clock : Clock.systemDefaultZone());
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("nombreTrabajador", nombreCompleto(uTrabajador, "Especialista"));
        vars.put("nombreCliente", uCliente != null ? valor(uCliente.getNombre(), "Clienta") : "Clienta");
        vars.put("codigoReserva", valor(cita.getCodigoReserva(), "RES-2026-000000"));
        vars.put("nombreServicio", servicio != null ? valor(servicio.getNombre(), "Servicio") : "Servicio");
        vars.put("fechaCita", cita.getFechaHoraInicio() != null ? cita.getFechaHoraInicio().format(FECHA_FMT) : ahora.format(FECHA_FMT));
        vars.put("horaCita", cita.getFechaHoraInicio() != null ? cita.getFechaHoraInicio().format(HORA_FMT) : "10:00");
        vars.put("motivoCancelacion", valor(cita.getMotivoCancelacion(), "Cancelación informada."));
        vars.put("urlReservar", frontendBaseUrl + "/admin/panel-trabajador");

        String asunto = "Cita cancelada (" + vars.get("codigoReserva") + ") — "
                + (uCliente != null ? (valor(uCliente.getNombre(), "") + " " + valor(uCliente.getApellidoP(), "")).trim() : "Cliente");
        String html = renderizarPlantilla("cita-cancelada.html", vars);
        return despacharCorreo("CITA_CANCELADA_TRABAJADOR", uTrabajador.getCorreo(),
                vars.get("nombreTrabajador"), asunto, vars.get("codigoReserva"), html);
    }

    /**
     * 6. Correo de Recordatorio 24 horas antes de la cita.
     */
    public CorreoEnviadoDTO enviarRecordatorio(Cita cita) {
        if (cita == null || cita.getCliente() == null || cita.getCliente().getUsuario() == null) return null;
        Usuario uCliente = cita.getCliente().getUsuario();
        Usuario uTrabajador = cita.getTrabajador() != null ? cita.getTrabajador().getUsuario() : null;
        Servicio servicio = cita.getServicio();

        LocalDateTime ahora = LocalDateTime.now(clock != null ? clock : Clock.systemDefaultZone());
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("nombreCliente", valor(uCliente.getNombre(), "Clienta"));
        vars.put("codigoReserva", valor(cita.getCodigoReserva(), "RES-2026-000000"));
        vars.put("nombreServicio", servicio != null ? valor(servicio.getNombre(), "Servicio") : "Servicio");
        vars.put("duracionMinutos", servicio != null && servicio.getDuracionMinutos() != null
                ? String.valueOf(servicio.getDuracionMinutos()) : "60");
        vars.put("nombreTrabajador", nombreCompleto(uTrabajador, "Especialista"));
        vars.put("fechaCita", cita.getFechaHoraInicio() != null ? cita.getFechaHoraInicio().format(FECHA_FMT) : ahora.format(FECHA_FMT));
        vars.put("horaCita", cita.getFechaHoraInicio() != null ? cita.getFechaHoraInicio().format(HORA_FMT) : "10:00");
        vars.put("urlMiPerfil", frontendBaseUrl + "/mi-perfil");

        String asunto = "Recordatorio: mañana tienes tu cita en Nicolett Studio Fantasy (" + vars.get("horaCita") + " hrs)";
        String html = renderizarPlantilla("recordatorio-cita.html", vars);
        return despacharCorreo("RECORDATORIO", uCliente.getCorreo(),
                nombreCompleto(uCliente, "Clienta"), asunto, vars.get("codigoReserva"), html);
    }

    private CorreoEnviadoDTO despacharCorreo(String tipo, String destinatario, String nombreDestinatario,
                                             String asunto, String codigoReserva, String html) {
        LocalDateTime ahora = LocalDateTime.now(clock != null ? clock : Clock.systemDefaultZone());
        String estadoEnvio = "SIMULADO_BANDEJA_LOCAL";
        String detalleEnvio = "Correo generado y guardado en la bandeja del sistema (configura MAIL_PASSWORD para despacho SMTP externo).";

        if (isSmtpConfigurado()) {
            try {
                JavaMailSenderImpl sender = crearMailSender();
                MimeMessage message = sender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
                helper.setFrom(mailUsername, "Nicolett Studio Fantasy");
                helper.setTo(destinatario);
                helper.setSubject(asunto);
                helper.setText(html, true);
                sender.send(message);
                estadoEnvio = "ENVIADO_SMTP";
                detalleEnvio = "Entregado al servidor SMTP (" + mailHost + ":" + mailPort + ") desde " + mailUsername;
                log.info("Correo [{}] enviado por SMTP exitosamente a {}", tipo, destinatario);
            } catch (Exception e) {
                String detalleError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                if (e.getCause() != null && e.getCause().getMessage() != null) {
                    detalleError += " -> " + e.getCause().getMessage();
                }
                estadoEnvio = "ERROR_SMTP";
                // El detalle técnico queda solo en el log del servidor.
                detalleEnvio = "No se pudo entregar al servidor SMTP. El correo quedó guardado en la bandeja local.";
                log.warn("No se pudo enviar por SMTP el correo [{}] a {}: {}", tipo, destinatario, detalleError, e);
            }
        } else {
            log.info("Correo [{}] registrado en bandeja local para destinatario={} asunto='{}'", tipo, destinatario, asunto);
        }

        CorreoEnviadoDTO dto = new CorreoEnviadoDTO(
                secuenciaId.getAndIncrement(),
                tipo,
                destinatario,
                nombreDestinatario,
                asunto,
                codigoReserva,
                estadoEnvio,
                detalleEnvio,
                ahora.format(FECHA_HORA_FMT),
                html
        );
        bandejaEnviados.add(0, dto);
        // La bandeja vive en memoria: se descartan los más antiguos para que no crezca sin límite.
        while (bandejaEnviados.size() > MAX_BANDEJA) {
            bandejaEnviados.remove(bandejaEnviados.size() - 1);
        }
        return dto;
    }

    private JavaMailSenderImpl crearMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mailHost);
        sender.setPort(mailPort);
        sender.setUsername(mailUsername);
        sender.setPassword(mailPassword != null ? mailPassword.replace(" ", "").trim() : "");
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", mailHost);
        props.put("mail.smtp.connectiontimeout", "7000");
        props.put("mail.smtp.timeout", "7000");
        props.put("mail.smtp.writetimeout", "7000");
        return sender;
    }

    private String renderizarPlantilla(String nombreArchivo, Map<String, String> variables) {
        String contenido = cargarPlantilla(nombreArchivo);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String marcador = "{{" + entry.getKey() + "}}";
            String valorSeguro = escaparHtml(entry.getValue());
            contenido = contenido.replace(marcador, valorSeguro);
        }
        return contenido;
    }

    private String cargarPlantilla(String nombreArchivo) {
        String ruta = "templates/correos/" + nombreArchivo;
        try (InputStream is = new ClassPathResource(ruta).getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("No se pudo cargar la plantilla {}: {}", ruta, e.getMessage());
            return "<html><body><h1>Nicolett Studio Fantasy</h1><p>Notificación automática.</p></body></html>";
        }
    }

    private static String escaparHtml(String texto) {
        if (texto == null) return "";
        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String valor(String v, String porDefecto) {
        return v != null && !v.isBlank() ? v.trim() : porDefecto;
    }

    private static String nombreCompleto(Usuario u, String porDefecto) {
        if (u == null) return porDefecto;
        String n = valor(u.getNombre(), "");
        String a = valor(u.getApellidoP(), "");
        String unido = (n + " " + a).trim();
        return unido.isEmpty() ? porDefecto : unido;
    }

    private static String formatearMoneda(BigDecimal monto) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));
        nf.setMaximumFractionDigits(0);
        return nf.format(monto != null ? monto : BigDecimal.ZERO);
    }
}
