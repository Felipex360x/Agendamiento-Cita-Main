package cl.nicolet.backend.service;

import cl.nicolet.backend.dto.CorreoEnviadoDTO;
import cl.nicolet.backend.model.*;
import cl.nicolet.backend.payment.WebpayException;
import cl.nicolet.backend.payment.WebpayPlusClient;
import cl.nicolet.backend.repository.*;
import cl.nicolet.backend.security.JwtUtils;
import cl.nicolet.backend.security.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Pago de citas con Webpay Plus: bloqueo del horario, aprobación, rechazo, anulación, vencimiento
 * y orden de los correos (administración primero, profesional después del pago).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PagoWebpayIntegrationTest {

    private static final String RETORNO = "/api/v2/nicolet/pagos/webpay/retorno";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TipoUsuarioRepository tipoUsuarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private TrabajadorRepository trabajadorRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private HorarioDisponibilidadRepository horarioRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private PagoService pagoService;
    @Autowired private EmailService emailService;

    @MockBean private WebpayPlusClient webpay;

    private String sufijo;
    private Usuario admin;
    private Servicio servicio;
    private Trabajador pro;
    private Cliente ana, beto;
    private LocalDate lunes;

    @BeforeEach
    void fixtures() {
        sufijo = UUID.randomUUID().toString().substring(0, 8);
        lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(1);
        admin = usuario("admin", "ADMINISTRADOR");
        servicio = new Servicio();
        servicio.setNombre("Servicio pago " + sufijo);
        servicio.setDuracionMinutos(60);
        servicio.setPrecio(new BigDecimal("10000.00"));
        servicio.setActivo(true);
        servicio = servicioRepository.save(servicio);
        pro = trabajador("pro");
        ana = cliente("ana");
        beto = cliente("beto");
        emailService.limpiarBandeja();
    }

    @AfterEach
    void limpiar() {
        List<Cita> citas = citaRepository.findByTrabajadorId(pro.getId());
        for (Cita c : citas) pagoRepository.deleteAll(pagoRepository.findByCitaIdOrderByIdAsc(c.getId()));
        citaRepository.deleteAll(citas);
        horarioRepository.deleteAll(horarioRepository.findByTrabajadorId(pro.getId()));
        trabajadorRepository.delete(pro);
        clienteRepository.deleteAll(List.of(ana, beto));
        servicioRepository.delete(servicio);
        for (Usuario u : new ArrayList<>(usuarioRepository.findAll())) {
            if (u.getCorreo().endsWith("@pagos." + sufijo + ".test")) usuarioRepository.delete(u);
        }
    }

    // Reserva y bloqueo

    @Test
    @DisplayName("Reservar con pago en línea bloquea el horario, no avisa a nadie y no se puede confirmar")
    void reservaOnlineBloqueaHorario() throws Exception {
        JsonId cita = reservar(ana, "ONLINE", 10);

        assertThat(cita.status).isEqualTo(201);
        assertThat(cita.json.get("estadoPago").asText()).isEqualTo("PENDIENTE_PAGO");
        assertThat(cita.json.get("estado").asText()).isEqualTo("PENDIENTE");
        assertThat(correosDe(cita.codigo)).isEmpty();

        // Otra persona no puede tomar la misma hora mientras se espera el pago.
        assertThat(reservar(beto, "ONLINE", 10).status).isEqualTo(400);

        // El profesional todavía no ve la cita ni se puede confirmar.
        assertThat(mockMvc.perform(get("/api/v2/nicolet/citas/trabajador/" + pro.getId()).header(HttpHeaders.AUTHORIZATION, token(admin)))
                .andReturn().getResponse().getContentAsString()).doesNotContain(cita.codigo);
        MvcResult confirmar = mockMvc.perform(patch("/api/v2/nicolet/citas/" + cita.id + "/estado")
                .header(HttpHeaders.AUTHORIZATION, token(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nuevoEstado\":\"CONFIRMADA\"}")).andReturn();
        assertThat(confirmar.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Reservar para pagar en el estudio avisa de inmediato al cliente y al profesional, sin comprobante de pago")
    void reservaLocal() throws Exception {
        JsonId cita = reservar(ana, null, 10);

        assertThat(cita.status).isEqualTo(201);
        assertThat(cita.json.get("estadoPago").asText()).isEqualTo("PAGO_EN_LOCAL");
        assertThat(correosDe(cita.codigo)).containsExactly("RESERVA_RECIBIDA", "NUEVA_CITA_TRABAJADOR");
    }

    // Pago aprobado

    @Test
    @DisplayName("Pago aprobado: cita pagada, y los correos salen en orden cliente → administración → profesional")
    void pagoAprobado() throws Exception {
        JsonId cita = reservar(ana, "ONLINE", 10);
        String token = iniciar(cita, ana);
        Pago pago = pagoRepository.findByCitaIdOrderByIdAsc(cita.id).get(0);
        assertThat(pago.getMonto()).isEqualTo(10000L);
        when(webpay.confirmar(token)).thenReturn(resultado(pago, "AUTHORIZED", 0));

        String destino = retorno("token_ws", token);

        assertThat(destino).contains("/pago-resultado.html").contains("estado=aprobado").contains("codigo=" + cita.codigo);
        Cita guardada = citaRepository.findById(cita.id).orElseThrow();
        assertThat(guardada.getEstadoPago()).isEqualTo(EstadoPago.PAGADO);
        assertThat(guardada.getEstado()).isEqualTo(EstadoCita.PENDIENTE);
        assertThat(pagoRepository.findById(pago.getId()).orElseThrow().getEstado()).isEqualTo(EstadoTransaccionPago.APROBADO);
        assertThat(correosDe(cita.codigo)).containsExactly("PAGO_RESERVA", "PAGO_ADMIN", "NUEVA_CITA_TRABAJADOR");

        // El aviso de administración llega UNA sola vez, a la cuenta principal (el administrador activo más antiguo).
        List<CorreoEnviadoDTO> avisos = emailService.listarCorreosEnviados().stream()
                .filter(c -> "PAGO_ADMIN".equals(c.getTipo()) && cita.codigo.equals(c.getCodigoReserva())).toList();
        assertThat(avisos).hasSize(1);
        assertThat(avisos.get(0).getDestinatario()).isEqualTo(adminPrincipalPorDefecto());

        // El profesional ya ve la cita y la puede confirmar.
        assertThat(mockMvc.perform(get("/api/v2/nicolet/citas/trabajador/" + pro.getId()).header(HttpHeaders.AUTHORIZATION, token(admin)))
                .andReturn().getResponse().getContentAsString()).contains(cita.codigo);
    }

    @Test
    @DisplayName("Si el cliente refresca la página de retorno no se vuelve a confirmar ni a enviar correos")
    void retornoRepetidoEsIdempotente() throws Exception {
        JsonId cita = reservar(ana, "ONLINE", 10);
        String token = iniciar(cita, ana);
        when(webpay.confirmar(token)).thenReturn(resultado(pagoRepository.findByCitaIdOrderByIdAsc(cita.id).get(0), "AUTHORIZED", 0));

        retorno("token_ws", token);
        String segundo = retorno("token_ws", token);

        assertThat(segundo).contains("estado=aprobado");
        verify(webpay, times(1)).confirmar(token);
        assertThat(correosDe(cita.codigo)).hasSize(3);
    }

    // Pago rechazado, anulado o vencido

    @Test
    @DisplayName("Pago rechazado: la cita se cancela, el horario queda libre y solo la clienta recibe el aviso de rechazo")
    void pagoRechazado() throws Exception {
        JsonId cita = reservar(ana, "ONLINE", 10);
        String token = iniciar(cita, ana);
        when(webpay.confirmar(token)).thenReturn(resultado(pagoRepository.findByCitaIdOrderByIdAsc(cita.id).get(0), "FAILED", -1));

        String destino = retorno("token_ws", token);

        assertThat(destino).contains("estado=rechazado");
        Cita guardada = citaRepository.findById(cita.id).orElseThrow();
        assertThat(guardada.getEstado()).isEqualTo(EstadoCita.CANCELADA);
        // Solo la clienta recibe el aviso de rechazo: ni administración ni el profesional.
        assertThat(correosDe(cita.codigo)).containsExactly("PAGO_RECHAZADO");
        assertThat(reservar(beto, "ONLINE", 10).status).isEqualTo(201);
    }

    @Test
    @DisplayName("El cliente anula el pago en Webpay: no se confirma nada y el horario queda libre")
    void pagoAnuladoPorElCliente() throws Exception {
        JsonId cita = reservar(ana, "ONLINE", 10);
        String token = iniciar(cita, ana);
        Pago pago = pagoRepository.findByCitaIdOrderByIdAsc(cita.id).get(0);

        String destino = retorno("TBK_TOKEN", token, "TBK_ORDEN_COMPRA", pago.getBuyOrder(), "TBK_ID_SESION", pago.getSessionId());

        assertThat(destino).contains("estado=anulado");
        verify(webpay, never()).confirmar(anyString());
        assertThat(citaRepository.findById(cita.id).orElseThrow().getEstado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(pagoRepository.findById(pago.getId()).orElseThrow().getEstado()).isEqualTo(EstadoTransaccionPago.ANULADO_POR_USUARIO);
        assertThat(correosDe(cita.codigo)).isEmpty();
    }

    @Test
    @DisplayName("Aviso de tiempo agotado: solo vale con el session_id correcto (nadie puede cancelar citas ajenas)")
    void timeoutDelFormulario() throws Exception {
        JsonId cita = reservar(ana, "ONLINE", 10);
        iniciar(cita, ana);
        Pago pago = pagoRepository.findByCitaIdOrderByIdAsc(cita.id).get(0);

        String falso = retorno("TBK_ORDEN_COMPRA", pago.getBuyOrder(), "TBK_ID_SESION", "adivinado");
        assertThat(falso).contains("estado=error");
        assertThat(citaRepository.findById(cita.id).orElseThrow().getEstado()).isEqualTo(EstadoCita.PENDIENTE);

        String real = retorno("TBK_ORDEN_COMPRA", pago.getBuyOrder(), "TBK_ID_SESION", pago.getSessionId());
        assertThat(real).contains("estado=expirado");
        assertThat(citaRepository.findById(cita.id).orElseThrow().getEstado()).isEqualTo(EstadoCita.CANCELADA);
    }

    @Test
    @DisplayName("Si vence el plazo sin pagar, la cita se cancela y un pago tardío NO se confirma con Webpay")
    void vencimientoDelPlazo() throws Exception {
        JsonId cita = reservar(ana, "ONLINE", 10);
        String token = iniciar(cita, ana);
        Cita c = citaRepository.findById(cita.id).orElseThrow();
        c.setPagoExpiraEn(LocalDateTime.now().minusMinutes(1));
        citaRepository.save(c);

        assertThat(pagoService.expirarPagosVencidos()).isEqualTo(1);

        assertThat(citaRepository.findById(cita.id).orElseThrow().getEstado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(pagoRepository.findByCitaIdOrderByIdAsc(cita.id).get(0).getEstado()).isEqualTo(EstadoTransaccionPago.EXPIRADO);
        assertThat(reservar(beto, "ONLINE", 10).status).isEqualTo(201);

        String tardio = retorno("token_ws", token);
        assertThat(tardio).contains("estado=expirado");
        verify(webpay, never()).confirmar(anyString());
        assertThat(correosDe(cita.codigo)).isEmpty();
    }

    @Test
    @DisplayName("Si no se supo el resultado de confirmar, se consulta a Webpay antes de decidir")
    void confirmarFallaPeroConsultaResuelve() throws Exception {
        JsonId cita = reservar(ana, "ONLINE", 10);
        String token = iniciar(cita, ana);
        Pago pago = pagoRepository.findByCitaIdOrderByIdAsc(cita.id).get(0);
        when(webpay.confirmar(token)).thenThrow(new WebpayException("sin respuesta", null, null));
        when(webpay.consultar(token)).thenReturn(resultado(pago, "AUTHORIZED", 0));

        assertThat(retorno("token_ws", token)).contains("estado=aprobado");
        assertThat(citaRepository.findById(cita.id).orElseThrow().getEstadoPago()).isEqualTo(EstadoPago.PAGADO);
    }

    @Test
    @DisplayName("Si no hay forma de saber si se pagó, la cita se cancela (Webpay no cobra lo que no se confirma)")
    void confirmarYConsultarFallan() throws Exception {
        JsonId cita = reservar(ana, "ONLINE", 10);
        String token = iniciar(cita, ana);
        when(webpay.confirmar(token)).thenThrow(new WebpayException("sin respuesta", null, null));
        when(webpay.consultar(token)).thenThrow(new WebpayException("sin respuesta", null, null));

        assertThat(retorno("token_ws", token)).contains("estado=error");
        assertThat(citaRepository.findById(cita.id).orElseThrow().getEstado()).isEqualTo(EstadoCita.CANCELADA);
    }

    // Iniciar el pago

    @Test
    @DisplayName("Solo el cliente dueño (o un administrador) puede iniciar el pago de una cita")
    void soloElDuenoInicia() throws Exception {
        JsonId cita = reservar(ana, "ONLINE", 10);
        when(webpay.crear(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new WebpayPlusClient.CrearRespuesta(tokenUnico(), "https://webpay.test/form"));

        int ajeno = mockMvc.perform(post("/api/v2/nicolet/pagos/webpay/citas/" + cita.id + "/iniciar")
                .header(HttpHeaders.AUTHORIZATION, token(beto.getUsuario()))).andReturn().getResponse().getStatus();
        int anonimo = mockMvc.perform(post("/api/v2/nicolet/pagos/webpay/citas/" + cita.id + "/iniciar")).andReturn().getResponse().getStatus();
        int propio = mockMvc.perform(post("/api/v2/nicolet/pagos/webpay/citas/" + cita.id + "/iniciar")
                .header(HttpHeaders.AUTHORIZATION, token(ana.getUsuario()))).andReturn().getResponse().getStatus();

        assertThat(ajeno).isEqualTo(403);
        assertThat(anonimo).isIn(401, 403);
        assertThat(propio).isEqualTo(200);
    }

    @Test
    @DisplayName("Si Webpay no responde al iniciar, se informa el error y se puede reintentar sin perder el horario")
    void webpayCaidoAlIniciar() throws Exception {
        JsonId cita = reservar(ana, "ONLINE", 10);
        when(webpay.crear(anyString(), anyString(), anyLong(), anyString())).thenThrow(new WebpayException("caído", null, null));

        int estado = mockMvc.perform(post("/api/v2/nicolet/pagos/webpay/citas/" + cita.id + "/iniciar")
                .header(HttpHeaders.AUTHORIZATION, token(ana.getUsuario()))).andReturn().getResponse().getStatus();

        assertThat(estado).isEqualTo(502);
        assertThat(pagoRepository.findByCitaIdOrderByIdAsc(cita.id)).isEmpty();
        assertThat(citaRepository.findById(cita.id).orElseThrow().getEstadoPago()).isEqualTo(EstadoPago.PENDIENTE_PAGO);

        reset(webpay);
        when(webpay.crear(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new WebpayPlusClient.CrearRespuesta(tokenUnico(), "https://webpay.test/form"));
        int reintento = mockMvc.perform(post("/api/v2/nicolet/pagos/webpay/citas/" + cita.id + "/iniciar")
                .header(HttpHeaders.AUTHORIZATION, token(ana.getUsuario()))).andReturn().getResponse().getStatus();
        assertThat(reintento).isEqualTo(200);
    }

    @Test
    @DisplayName("Tocar dos veces 'pagar' reutiliza el mismo pago en vez de crear otro")
    void iniciarDosVecesReutiliza() throws Exception {
        JsonId cita = reservar(ana, "ONLINE", 10);
        String primero = iniciar(cita, ana);
        String segundo = iniciar(cita, ana);

        assertThat(segundo).isEqualTo(primero);
        assertThat(pagoRepository.findByCitaIdOrderByIdAsc(cita.id)).hasSize(1);
        verify(webpay, times(1)).crear(anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Un retorno que no corresponde a ningún pago termina en error, sin romper nada")
    void retornoDesconocido() throws Exception {
        assertThat(retorno("token_ws", "no-existe")).contains("estado=error");
        assertThat(retorno()).contains("estado=error");
        verify(webpay, never()).confirmar(anyString());
    }

    // Correos de pago

    @Test
    @DisplayName("El correo de pago rechazado va a la clienta, con su reserva y sin cobro, y no se repite si refresca")
    void correoDePagoRechazado() throws Exception {
        JsonId cita = reservar(ana, "ONLINE", 10);
        String token = iniciar(cita, ana);
        when(webpay.confirmar(token)).thenReturn(resultado(pagoRepository.findByCitaIdOrderByIdAsc(cita.id).get(0), "FAILED", -1));

        retorno("token_ws", token);
        retorno("token_ws", token); // la clienta refresca la página de resultado

        List<CorreoEnviadoDTO> correos = emailService.listarCorreosEnviados().stream()
                .filter(c -> cita.codigo.equals(c.getCodigoReserva())).toList();
        assertThat(correos).hasSize(1);
        CorreoEnviadoDTO correo = correos.get(0);
        assertThat(correo.getTipo()).isEqualTo("PAGO_RECHAZADO");
        assertThat(correo.getDestinatario()).isEqualTo(ana.getUsuario().getCorreo());
        assertThat(correo.getAsunto()).contains("Pago rechazado").contains(cita.codigo);
        assertThat(correo.getHtml()).contains("No pudimos procesar tu pago").contains("No se hizo ningún cobro")
                .contains(cita.codigo).contains("$10.000").contains("/reservar.html").doesNotContain("{{");
    }

    @Test
    @DisplayName("Si la clienta anula el pago, o se agota el tiempo, o falla Webpay, no se manda correo de rechazo")
    void soloElRechazoDelBancoEnviaCorreoDeRechazo() throws Exception {
        JsonId anulada = reservar(ana, "ONLINE", 10);
        String tokenAnulada = iniciar(anulada, ana);
        retorno("TBK_TOKEN", tokenAnulada);

        JsonId conError = reservar(beto, "ONLINE", 11);
        String tokenError = iniciar(conError, beto);
        when(webpay.confirmar(tokenError)).thenThrow(new WebpayException("sin respuesta", null, null));
        when(webpay.consultar(tokenError)).thenThrow(new WebpayException("sin respuesta", null, null));
        retorno("token_ws", tokenError);

        assertThat(correosDe(anulada.codigo)).isEmpty();
        assertThat(correosDe(conError.codigo)).isEmpty();
    }

    @Test
    @DisplayName("El aviso de pago confirmado va solo al correo configurado como cuenta principal de administración")
    void avisoAdministracionAlCorreoConfigurado() throws Exception {
        String anterior = (String) org.springframework.test.util.ReflectionTestUtils.getField(pagoService, "correosAdminConfigurados");
        org.springframework.test.util.ReflectionTestUtils.setField(pagoService, "correosAdminConfigurados", "principal@" + sufijo + ".estudio.test");
        try {
            JsonId cita = reservar(ana, "ONLINE", 10);
            String token = iniciar(cita, ana);
            when(webpay.confirmar(token)).thenReturn(resultado(pagoRepository.findByCitaIdOrderByIdAsc(cita.id).get(0), "AUTHORIZED", 0));

            retorno("token_ws", token);

            List<CorreoEnviadoDTO> avisos = emailService.listarCorreosEnviados().stream()
                    .filter(c -> "PAGO_ADMIN".equals(c.getTipo()) && cita.codigo.equals(c.getCodigoReserva())).toList();
            assertThat(avisos).extracting(CorreoEnviadoDTO::getDestinatario).containsExactly("principal@" + sufijo + ".estudio.test");
            // La clienta recibe su comprobante y el profesional su aviso; ninguno ve el correo de administración.
            assertThat(correosDe(cita.codigo)).containsExactly("PAGO_RESERVA", "PAGO_ADMIN", "NUEVA_CITA_TRABAJADOR");
            assertThat(emailService.listarCorreosEnviados().stream()
                    .filter(c -> "PAGO_RESERVA".equals(c.getTipo()) && cita.codigo.equals(c.getCodigoReserva()))
                    .map(CorreoEnviadoDTO::getDestinatario)).containsExactly(ana.getUsuario().getCorreo());
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(pagoService, "correosAdminConfigurados", anterior);
        }
    }

    @Test
    @DisplayName("Sin correo configurado, administración usa APP_ADMIN_EMAIL (el primer administrador)")
    void avisoAdministracionUsaElCorreoDelPrimerAdministrador() {
        String anterior = (String) org.springframework.test.util.ReflectionTestUtils.getField(pagoService, "correoAdminPrincipal");
        org.springframework.test.util.ReflectionTestUtils.setField(pagoService, "correoAdminPrincipal", "  jefa@" + sufijo + ".estudio.test ");
        try {
            assertThat(pagoService.destinatariosAdministracion()).containsExactly("jefa@" + sufijo + ".estudio.test");
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(pagoService, "correoAdminPrincipal", anterior);
        }
    }

    // Historial de pagos (administración)

    @Test
    @DisplayName("El historial de pagos lo ve solo administración y no expone el token ni el session_id")
    void historialSoloParaAdministracion() throws Exception {
        JsonId cita = reservar(ana, "ONLINE", 10);
        String token = iniciar(cita, ana);
        Pago pago = pagoRepository.findByCitaIdOrderByIdAsc(cita.id).get(0);
        when(webpay.confirmar(token)).thenReturn(resultado(pago, "AUTHORIZED", 0));
        retorno("token_ws", token);

        String url = "/api/v2/nicolet/pagos/webpay/historial";
        assertThat(mockMvc.perform(get(url)).andReturn().getResponse().getStatus()).isIn(401, 403);
        assertThat(mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, token(ana.getUsuario()))).andReturn().getResponse().getStatus()).isEqualTo(403);
        assertThat(mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, token(pro.getUsuario()))).andReturn().getResponse().getStatus()).isEqualTo(403);

        MvcResult r = mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, token(admin))).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        String cuerpo = r.getResponse().getContentAsString();
        assertThat(cuerpo).doesNotContain(token).doesNotContain(pago.getSessionId()).doesNotContain("urlPago").doesNotContain("sessionId");
        var fila = mapper.readTree(cuerpo).get("pagos").get(0);
        assertThat(fila.get("codigoReserva").asText()).isEqualTo(cita.codigo);
        assertThat(fila.get("estado").asText()).isEqualTo("APROBADO");
        assertThat(fila.get("monto").asLong()).isEqualTo(10000L);
        assertThat(fila.get("clienteCorreo").asText()).isEqualTo(ana.getUsuario().getCorreo());
        assertThat(fila.get("ultimos4Digitos").asText()).isEqualTo("6623");
        assertThat(fila.get("codigoAutorizacion").asText()).isEqualTo("1213");
        assertThat(fila.get("tipoPago").asText()).contains("VN");
        assertThat(mapper.readTree(cuerpo).get("truncado").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("El historial filtra por estado y por fecha, y un rango al revés se rechaza")
    void historialFiltros() throws Exception {
        JsonId aprobada = reservar(ana, "ONLINE", 10);
        String tokenA = iniciar(aprobada, ana);
        when(webpay.confirmar(tokenA)).thenReturn(resultado(pagoRepository.findByCitaIdOrderByIdAsc(aprobada.id).get(0), "AUTHORIZED", 0));
        retorno("token_ws", tokenA);
        JsonId rechazada = reservar(beto, "ONLINE", 11);
        String tokenR = iniciar(rechazada, beto);
        when(webpay.confirmar(tokenR)).thenReturn(resultado(pagoRepository.findByCitaIdOrderByIdAsc(rechazada.id).get(0), "FAILED", -1));
        retorno("token_ws", tokenR);

        String base = "/api/v2/nicolet/pagos/webpay/historial";
        String hoy = LocalDate.now().toString();
        String soloRechazados = mockMvc.perform(get(base + "?estado=RECHAZADO").header(HttpHeaders.AUTHORIZATION, token(admin)))
                .andReturn().getResponse().getContentAsString();
        assertThat(soloRechazados).contains(rechazada.codigo).doesNotContain(aprobada.codigo);
        // Los datos de tarjeta solo se muestran en pagos aprobados.
        assertThat(mapper.readTree(soloRechazados).get("pagos").get(0).get("ultimos4Digitos").isNull()).isTrue();

        String hoyAprobados = mockMvc.perform(get(base + "?estado=APROBADO&desde=" + hoy + "&hasta=" + hoy).header(HttpHeaders.AUTHORIZATION, token(admin)))
                .andReturn().getResponse().getContentAsString();
        assertThat(hoyAprobados).contains(aprobada.codigo).doesNotContain(rechazada.codigo);

        String ayer = mockMvc.perform(get(base + "?hasta=" + LocalDate.now().minusDays(1)).header(HttpHeaders.AUTHORIZATION, token(admin)))
                .andReturn().getResponse().getContentAsString();
        assertThat(ayer).doesNotContain(aprobada.codigo).doesNotContain(rechazada.codigo);

        assertThat(mockMvc.perform(get(base + "?desde=" + hoy + "&hasta=" + LocalDate.now().minusDays(3)).header(HttpHeaders.AUTHORIZATION, token(admin)))
                .andReturn().getResponse().getStatus()).isEqualTo(400);
        assertThat(mockMvc.perform(get(base + "?estado=INVENTADO").header(HttpHeaders.AUTHORIZATION, token(admin)))
                .andReturn().getResponse().getStatus()).isEqualTo(400);
    }

    // Casos límite y manipulación

    @Test
    @DisplayName("Webpay aprueba, pero con un monto distinto al de la reserva: no se acepta y se cancela")
    void aprobadoConMontoDistinto() throws Exception {
        JsonId cita = reservar(ana, "ONLINE", 10);
        String token = iniciar(cita, ana);
        Pago pago = pagoRepository.findByCitaIdOrderByIdAsc(cita.id).get(0);
        WebpayPlusClient.Resultado ok = resultado(pago, "AUTHORIZED", 0);
        when(webpay.confirmar(token)).thenReturn(new WebpayPlusClient.Resultado(ok.vci(), new BigDecimal("100"), ok.status(),
                ok.buyOrder(), ok.sessionId(), ok.cardDetail(), ok.accountingDate(), ok.transactionDate(),
                ok.authorizationCode(), ok.paymentTypeCode(), ok.responseCode(), ok.installmentsNumber()));

        assertThat(retorno("token_ws", token)).contains("estado=error");

        Cita guardada = citaRepository.findById(cita.id).orElseThrow();
        assertThat(guardada.getEstado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(guardada.getEstadoPago()).isNotEqualTo(EstadoPago.PAGADO);
        assertThat(pagoRepository.findById(pago.getId()).orElseThrow().getEstado()).isEqualTo(EstadoTransaccionPago.ERROR);
        assertThat(correosDe(cita.codigo)).isEmpty();
    }

    @Test
    @DisplayName("Webpay aprueba, pero con una orden de compra de otra reserva: no se acepta")
    void aprobadoConOrdenDistinta() throws Exception {
        JsonId cita = reservar(ana, "ONLINE", 10);
        String token = iniciar(cita, ana);
        Pago pago = pagoRepository.findByCitaIdOrderByIdAsc(cita.id).get(0);
        WebpayPlusClient.Resultado ok = resultado(pago, "AUTHORIZED", 0);
        when(webpay.confirmar(token)).thenReturn(new WebpayPlusClient.Resultado(ok.vci(), ok.amount(), ok.status(),
                "RES-2026-OTRA-1", ok.sessionId(), ok.cardDetail(), ok.accountingDate(), ok.transactionDate(),
                ok.authorizationCode(), ok.paymentTypeCode(), ok.responseCode(), ok.installmentsNumber()));

        assertThat(retorno("token_ws", token)).contains("estado=error");
        assertThat(citaRepository.findById(cita.id).orElseThrow().getEstadoPago()).isNotEqualTo(EstadoPago.PAGADO);
        assertThat(correosDe(cita.codigo)).isEmpty();
    }

    @Test
    @DisplayName("Webpay responde AUTHORIZED pero con response_code distinto de 0: se trata como rechazado")
    void autorizadoConCodigoDeRespuestaMalo() throws Exception {
        JsonId cita = reservar(ana, "ONLINE", 10);
        String token = iniciar(cita, ana);
        when(webpay.confirmar(token)).thenReturn(resultado(pagoRepository.findByCitaIdOrderByIdAsc(cita.id).get(0), "AUTHORIZED", -4));

        assertThat(retorno("token_ws", token)).contains("estado=rechazado");
        assertThat(citaRepository.findById(cita.id).orElseThrow().getEstadoPago()).isNotEqualTo(EstadoPago.PAGADO);
    }

    @Test
    @DisplayName("Consultar devuelve una transacción todavía INITIALIZED: no cuenta como pagada")
    void consultaInicializadaNoEsPago() throws Exception {
        JsonId cita = reservar(ana, "ONLINE", 10);
        String token = iniciar(cita, ana);
        Pago pago = pagoRepository.findByCitaIdOrderByIdAsc(cita.id).get(0);
        when(webpay.confirmar(token)).thenThrow(new WebpayException("sin respuesta", null, null));
        when(webpay.consultar(token)).thenReturn(resultado(pago, "INITIALIZED", 0));

        assertThat(retorno("token_ws", token)).contains("estado=error");
        assertThat(citaRepository.findById(cita.id).orElseThrow().getEstado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(correosDe(cita.codigo)).isEmpty();
    }

    @Test
    @DisplayName("Si llegan token_ws y TBK_TOKEN a la vez, manda la anulación y no se confirma nada")
    void tokenWsYTbkTokenJuntos() throws Exception {
        JsonId cita = reservar(ana, "ONLINE", 10);
        String token = iniciar(cita, ana);
        when(webpay.confirmar(token)).thenReturn(resultado(pagoRepository.findByCitaIdOrderByIdAsc(cita.id).get(0), "AUTHORIZED", 0));

        assertThat(retorno("token_ws", token, "TBK_TOKEN", token)).contains("estado=anulado");
        verify(webpay, never()).confirmar(anyString());
        assertThat(citaRepository.findById(cita.id).orElseThrow().getEstadoPago()).isNotEqualTo(EstadoPago.PAGADO);
    }

    @Test
    @DisplayName("Orden de una reserva con session_id de otra: se rechaza y ninguna de las dos se cancela")
    void ordenConSesionDeOtraReserva() throws Exception {
        JsonId citaAna = reservar(ana, "ONLINE", 10);
        JsonId citaBeto = reservar(beto, "ONLINE", 11);
        iniciar(citaAna, ana);
        iniciar(citaBeto, beto);
        Pago pagoAna = pagoRepository.findByCitaIdOrderByIdAsc(citaAna.id).get(0);
        Pago pagoBeto = pagoRepository.findByCitaIdOrderByIdAsc(citaBeto.id).get(0);

        assertThat(retorno("TBK_ORDEN_COMPRA", pagoAna.getBuyOrder(), "TBK_ID_SESION", pagoBeto.getSessionId())).contains("estado=error");

        assertThat(citaRepository.findById(citaAna.id).orElseThrow().getEstado()).isEqualTo(EstadoCita.PENDIENTE);
        assertThat(citaRepository.findById(citaBeto.id).orElseThrow().getEstado()).isEqualTo(EstadoCita.PENDIENTE);
    }

    @Test
    @DisplayName("Un token válido de una reserva no sirve para pagar otra: cada token aprueba solo su reserva")
    void tokenAjenoNoPagaOtraReserva() throws Exception {
        JsonId citaAna = reservar(ana, "ONLINE", 10);
        JsonId citaBeto = reservar(beto, "ONLINE", 11);
        String tokenAna = iniciar(citaAna, ana);
        iniciar(citaBeto, beto);
        when(webpay.confirmar(tokenAna)).thenReturn(resultado(pagoRepository.findByCitaIdOrderByIdAsc(citaAna.id).get(0), "AUTHORIZED", 0));

        assertThat(retorno("token_ws", tokenAna)).contains("codigo=" + citaAna.codigo).doesNotContain(citaBeto.codigo);

        assertThat(citaRepository.findById(citaAna.id).orElseThrow().getEstadoPago()).isEqualTo(EstadoPago.PAGADO);
        assertThat(citaRepository.findById(citaBeto.id).orElseThrow().getEstadoPago()).isEqualTo(EstadoPago.PENDIENTE_PAGO);
        assertThat(correosDe(citaBeto.codigo)).isEmpty();
    }

    @Test
    @DisplayName("No se puede iniciar el pago de una cita ya pagada, cancelada o con el plazo vencido")
    void noSePuedeIniciarFueraDeEstado() throws Exception {
        // Ya pagada
        JsonId pagada = reservar(ana, "ONLINE", 10);
        String token = iniciar(pagada, ana);
        when(webpay.confirmar(token)).thenReturn(resultado(pagoRepository.findByCitaIdOrderByIdAsc(pagada.id).get(0), "AUTHORIZED", 0));
        retorno("token_ws", token);
        assertThat(iniciarEstadoHttp(pagada, ana)).isEqualTo(409);

        // Cancelada por rechazo
        JsonId rechazada = reservar(beto, "ONLINE", 12);
        String tokenRechazo = iniciar(rechazada, beto);
        when(webpay.confirmar(tokenRechazo)).thenReturn(resultado(pagoRepository.findByCitaIdOrderByIdAsc(rechazada.id).get(0), "FAILED", -1));
        retorno("token_ws", tokenRechazo);
        assertThat(iniciarEstadoHttp(rechazada, beto)).isEqualTo(409);

        // Plazo vencido (todavía sin que el job la cancele)
        JsonId vencida = reservar(ana, "ONLINE", 14);
        Cita c = citaRepository.findById(vencida.id).orElseThrow();
        c.setPagoExpiraEn(LocalDateTime.now().minusSeconds(5));
        citaRepository.save(c);
        assertThat(iniciarEstadoHttp(vencida, ana)).isEqualTo(409);

        // Cita para pagar en el estudio: no tiene pago en línea
        JsonId local = reservar(ana, null, 16);
        assertThat(iniciarEstadoHttp(local, ana)).isEqualTo(409);
    }

    @Test
    @DisplayName("El job de vencimiento no toca citas ya pagadas ni citas con plazo vigente")
    void vencimientoNoTocaPagadasNiVigentes() throws Exception {
        JsonId pagada = reservar(ana, "ONLINE", 10);
        String token = iniciar(pagada, ana);
        when(webpay.confirmar(token)).thenReturn(resultado(pagoRepository.findByCitaIdOrderByIdAsc(pagada.id).get(0), "AUTHORIZED", 0));
        retorno("token_ws", token);
        JsonId vigente = reservar(beto, "ONLINE", 11);

        assertThat(pagoService.expirarPagosVencidos()).isZero();

        assertThat(citaRepository.findById(pagada.id).orElseThrow().getEstado()).isEqualTo(EstadoCita.PENDIENTE);
        assertThat(citaRepository.findById(pagada.id).orElseThrow().getEstadoPago()).isEqualTo(EstadoPago.PAGADO);
        assertThat(citaRepository.findById(vigente.id).orElseThrow().getEstado()).isEqualTo(EstadoCita.PENDIENTE);
    }

    @Test
    @DisplayName("Dos retornos simultáneos del mismo pago confirman con Webpay una sola vez y envían los correos una sola vez")
    void retornosSimultaneosConfirmanUnaVez() throws Exception {
        JsonId cita = reservar(ana, "ONLINE", 10);
        String token = iniciar(cita, ana);
        when(webpay.confirmar(token)).thenAnswer(inv -> {
            Thread.sleep(300); // da tiempo a que el segundo retorno llegue mientras el primero confirma
            return resultado(pagoRepository.findByCitaIdOrderByIdAsc(cita.id).get(0), "AUTHORIZED", 0);
        });

        List<String> destinos = lanzarEnParalelo(2, () -> retorno("token_ws", token));

        assertThat(destinos).allSatisfy(d -> assertThat(d).contains("estado=aprobado"));
        verify(webpay, times(1)).confirmar(token);
        assertThat(correosDe(cita.codigo)).containsExactly("PAGO_RESERVA", "PAGO_ADMIN", "NUEVA_CITA_TRABAJADOR");
    }

    @Test
    @DisplayName("Carrera entre el pago y el vencimiento: nunca queda una cita pagada y cancelada a la vez")
    void carreraPagoContraVencimiento() throws Exception {
        for (int vuelta = 0; vuelta < 3; vuelta++) {
            reset(webpay);
            emailService.limpiarBandeja();
            JsonId cita = reservar(ana, "ONLINE", 9 + vuelta);
            String token = iniciar(cita, ana);
            Pago pago = pagoRepository.findByCitaIdOrderByIdAsc(cita.id).get(0);
            when(webpay.confirmar(token)).thenReturn(resultado(pago, "AUTHORIZED", 0));
            // El plazo vence justo ahora: tanto el retorno como el job intentan resolver la cita.
            Cita c = citaRepository.findById(cita.id).orElseThrow();
            c.setPagoExpiraEn(LocalDateTime.now().plusNanos(50_000_000));
            citaRepository.save(c);
            Thread.sleep(60);

            lanzarEnParalelo(2, () -> {
                if (Thread.currentThread().getName().endsWith("0")) return retorno("token_ws", token);
                return String.valueOf(pagoService.expirarPagosVencidos());
            });

            Cita final_ = citaRepository.findById(cita.id).orElseThrow();
            // El plazo ya había vencido: el pago no debe confirmarse y la cita queda cancelada, sin correos.
            assertThat(final_.getEstado()).isEqualTo(EstadoCita.CANCELADA);
            assertThat(final_.getEstadoPago()).isNotEqualTo(EstadoPago.PAGADO);
            assertThat(correosDe(cita.codigo)).isEmpty();
            verify(webpay, never()).confirmar(anyString());
        }
    }

    @Test
    @DisplayName("Los montos que Webpay no admite (decimales, cero, negativo, vacío) se rechazan antes de cobrar")
    void montosInvalidos() {
        assertThat(PagoService.montoEnPesos(citaConPrecio("10000.00"))).isEqualTo(10000L);
        assertThat(PagoService.montoEnPesos(citaConPrecio("1"))).isEqualTo(1L);
        for (String malo : new String[]{"10000.50", "0", "0.00", "-500", null}) {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                    () -> PagoService.montoEnPesos(citaConPrecio(malo)), "monto " + malo);
        }
    }

    // Funciones de ayuda

    /** Sin configuración, la cuenta principal es el administrador activo más antiguo. */
    private String adminPrincipalPorDefecto() {
        return usuarioRepository.findByTipoUsuarioNombreIgnoreCase("ADMINISTRADOR").stream()
                .filter(a -> a.getEstado() == Usuario.ESTADO_ACTIVO && a.getCorreo() != null)
                .min(Comparator.comparing(Usuario::getId)).orElseThrow().getCorreo();
    }

    private static Cita citaConPrecio(String precio) {
        Cita c = new Cita();
        c.setPrecioFinal(precio == null ? null : new BigDecimal(precio));
        return c;
    }

    private int iniciarEstadoHttp(JsonId cita, Cliente c) throws Exception {
        when(webpay.crear(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new WebpayPlusClient.CrearRespuesta(tokenUnico(), "https://webpay.test/form"));
        return mockMvc.perform(post("/api/v2/nicolet/pagos/webpay/citas/" + cita.id + "/iniciar")
                .header(HttpHeaders.AUTHORIZATION, token(c.getUsuario()))).andReturn().getResponse().getStatus();
    }

    @FunctionalInterface
    private interface Tarea { String ejecutar() throws Exception; }

    /** Lanza la misma tarea en {@code n} hilos a la vez (hilos "tarea-0", "tarea-1"…) y devuelve sus resultados. */
    private List<String> lanzarEnParalelo(int n, Tarea tarea) throws Exception {
        var pool = java.util.concurrent.Executors.newFixedThreadPool(n, new java.util.concurrent.ThreadFactory() {
            private int i = 0;
            public Thread newThread(Runnable r) { return new Thread(r, "tarea-" + (i++)); }
        });
        try {
            var listos = new java.util.concurrent.CountDownLatch(n);
            var salida = new java.util.concurrent.CountDownLatch(1);
            List<java.util.concurrent.Future<String>> futuros = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                futuros.add(pool.submit(() -> {
                    listos.countDown();
                    salida.await();
                    return tarea.ejecutar();
                }));
            }
            listos.await();
            salida.countDown();
            List<String> resultados = new ArrayList<>();
            for (var f : futuros) resultados.add(f.get(30, java.util.concurrent.TimeUnit.SECONDS));
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }

    private record JsonId(int status, long id, String codigo, com.fasterxml.jackson.databind.JsonNode json) { }

    private JsonId reservar(Cliente c, String modalidad, int hora) throws Exception {
        String body = "{\"clienteId\":" + c.getId() + ",\"trabajadorId\":" + pro.getId() + ",\"servicioId\":" + servicio.getId()
                + ",\"fechaHoraInicio\":\"" + lunes.atTime(hora, 0) + "\""
                + (modalidad == null ? "" : ",\"modalidadPago\":\"" + modalidad + "\"") + "}";
        MvcResult r = mockMvc.perform(post("/api/v2/nicolet/citas").header(HttpHeaders.AUTHORIZATION, token(c.getUsuario()))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
        int status = r.getResponse().getStatus();
        if (status != 201) return new JsonId(status, 0, null, null);
        var json = mapper.readTree(r.getResponse().getContentAsString());
        return new JsonId(status, json.get("id").asLong(), json.get("codigoReserva").asText(), json);
    }

    /** Inicia el pago (con Webpay simulado) y devuelve el token. */
    private String iniciar(JsonId cita, Cliente c) throws Exception {
        if (pagoRepository.countByCitaId(cita.id) == 0) {
            when(webpay.crear(anyString(), anyString(), anyLong(), anyString()))
                    .thenReturn(new WebpayPlusClient.CrearRespuesta(tokenUnico(), "https://webpay.test/form"));
        }
        MvcResult r = mockMvc.perform(post("/api/v2/nicolet/pagos/webpay/citas/" + cita.id + "/iniciar")
                .header(HttpHeaders.AUTHORIZATION, token(c.getUsuario()))).andReturn();
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        return mapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private String tokenUnico() {
        return (UUID.randomUUID().toString() + UUID.randomUUID()).replace("-", "").substring(0, 64);
    }

    /** Hace la llamada de retorno de Webpay (sin sesión) y devuelve la dirección a la que redirige. */
    private String retorno(String... parametros) throws Exception {
        var peticion = get(RETORNO);
        for (int i = 0; i < parametros.length; i += 2) peticion = peticion.param(parametros[i], parametros[i + 1]);
        MvcResult r = mockMvc.perform(peticion).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(303);
        return r.getResponse().getHeader(HttpHeaders.LOCATION);
    }

    private WebpayPlusClient.Resultado resultado(Pago pago, String estado, int codigo) {
        return new WebpayPlusClient.Resultado("TSY", new BigDecimal(pago.getMonto()), estado, pago.getBuyOrder(), pago.getSessionId(),
                new WebpayPlusClient.DetalleTarjeta("6623"), "1120", "2026-11-20T15:00:00.000Z", "1213", "VN", codigo, 0);
    }

    /**
     * Tipos de correo enviados para la reserva, en el orden en que salieron. El aviso a administración
     * sale una vez por cada administrador activo; aquí se cuenta como un solo paso.
     */
    private List<String> correosDe(String codigoReserva) {
        List<String> tipos = new ArrayList<>();
        for (CorreoEnviadoDTO c : emailService.listarCorreosEnviados()) {
            if (!codigoReserva.equals(c.getCodigoReserva())) continue;
            if ("PAGO_ADMIN".equals(c.getTipo()) && !tipos.isEmpty() && "PAGO_ADMIN".equals(tipos.get(0))) continue;
            tipos.add(0, c.getTipo()); // la bandeja trae lo más nuevo primero
        }
        return tipos;
    }

    private String token(Usuario u) {
        return "Bearer " + jwtUtils.generarToken(UserDetailsImpl.build(u));
    }

    private Usuario usuario(String alias, String rol) {
        Usuario u = new Usuario();
        u.setNombre(alias);
        u.setApellidoP("Pago");
        u.setCorreo(alias + "@pagos." + sufijo + ".test");
        u.setPassword(passwordEncoder.encode("Clave-Segura-123"));
        u.setTipoUsuario(tipoUsuarioRepository.findByNombreIgnoreCase(rol).orElseThrow());
        return usuarioRepository.save(u);
    }

    private Trabajador trabajador(String alias) {
        Trabajador t = new Trabajador();
        t.setUsuario(usuario(alias, "PROFESIONAL"));
        t.setCargoEspecialidad("Estilista");
        t.setActivo(true);
        t.setServicios(new HashSet<>(Set.of(servicio)));
        t = trabajadorRepository.save(t);
        for (int dia = 1; dia <= 7; dia++) {
            HorarioDisponibilidad h = new HorarioDisponibilidad();
            h.setTrabajador(t);
            h.setDiaSemana(dia);
            h.setHoraInicio(LocalTime.of(9, 0));
            h.setHoraFin(LocalTime.of(18, 0));
            h.setActivo(true);
            horarioRepository.save(h);
        }
        return t;
    }

    private Cliente cliente(String alias) {
        Cliente c = new Cliente();
        c.setUsuario(usuario(alias, "CLIENTE"));
        c.setActivo(true);
        return clienteRepository.save(c);
    }
}
