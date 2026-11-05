package cl.nicolet.backend.controller;

import cl.nicolet.backend.dto.CorreoEnviadoDTO;
import cl.nicolet.backend.model.Cita;
import cl.nicolet.backend.model.Cliente;
import cl.nicolet.backend.model.EstadoCita;
import cl.nicolet.backend.model.Servicio;
import cl.nicolet.backend.model.Trabajador;
import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/nicolet/correos")
public class CorreoController {

    @Autowired
    private EmailService emailService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenerBandejaYEstado() {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("smtpConfigurado", emailService.isSmtpConfigurado());
        respuesta.put("remitente", emailService.getMailUsername());
        respuesta.put("host", emailService.getMailHost());
        respuesta.put("port", emailService.getMailPort());
        respuesta.put("correos", emailService.listarCorreosEnviados());
        return ResponseEntity.ok(respuesta);
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> limpiarBandeja() {
        emailService.limpiarBandeja();
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("mensaje", "Bandeja de correos vaciada.");
        respuesta.put("correos", emailService.listarCorreosEnviados());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/configurar-smtp")
    public ResponseEntity<Map<String, Object>> configurarSmtp(@RequestBody Map<String, Object> payload) {
        String host = payload.get("host") != null ? String.valueOf(payload.get("host")) : null;
        Integer port = null;
        if (payload.get("port") != null) {
            try {
                port = Integer.parseInt(String.valueOf(payload.get("port")));
            } catch (NumberFormatException ignored) {
            }
        }
        String remitente = payload.get("remitente") != null ? String.valueOf(payload.get("remitente")) : null;
        String appPassword = payload.get("appPassword") != null ? String.valueOf(payload.get("appPassword")) : null;

        emailService.configurarSmtp(host, port, remitente, appPassword);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("smtpConfigurado", emailService.isSmtpConfigurado());
        respuesta.put("remitente", emailService.getMailUsername());
        respuesta.put("mensaje", emailService.isSmtpConfigurado()
                ? "Credenciales SMTP activas para " + emailService.getMailUsername()
                : "Modo bandeja local activo (sin contraseña SMTP).");
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/probar-flujo")
    public ResponseEntity<Map<String, Object>> probarFlujoCorreos(@RequestBody(required = false) Map<String, String> body) {
        String correoCliente = body != null && body.get("correoCliente") != null && !body.get("correoCliente").isBlank()
                ? body.get("correoCliente").trim()
                : "nacuna888@gmail.com";
        String nombreCliente = body != null && body.get("nombreCliente") != null && !body.get("nombreCliente").isBlank()
                ? body.get("nombreCliente").trim()
                : "Nicolás";
        String apellidoCliente = body != null && body.get("apellidoCliente") != null && !body.get("apellidoCliente").isBlank()
                ? body.get("apellidoCliente").trim()
                : "Acuña";

        String correoTrabajador = body != null && body.get("correoTrabajador") != null && !body.get("correoTrabajador").isBlank()
                ? body.get("correoTrabajador").trim()
                : "acunafelipe272@gmail.com";
        String nombreTrabajador = body != null && body.get("nombreTrabajador") != null && !body.get("nombreTrabajador").isBlank()
                ? body.get("nombreTrabajador").trim()
                : "Felipe";
        String apellidoTrabajador = body != null && body.get("apellidoTrabajador") != null && !body.get("apellidoTrabajador").isBlank()
                ? body.get("apellidoTrabajador").trim()
                : "Acuña";

        Usuario uCliente = new Usuario(9001L, nombreCliente, apellidoCliente, correoCliente, "");
        Cliente cliente = new Cliente(9001L, uCliente, "+56940322551", "18.456.789-0", null, null, true);

        Usuario uTrabajador = new Usuario(9002L, nombreTrabajador, apellidoTrabajador, correoTrabajador, "");
        Servicio servicio = new Servicio(9001L, "Balayage + Hidratación Profunda", "Coloración y nutrición capilar", 90, new BigDecimal("45000"), true);
        Trabajador trabajador = new Trabajador();
        trabajador.setId(9002L);
        trabajador.setUsuario(uTrabajador);
        trabajador.setCargoEspecialidad("Especialista en Coloración y Estilismo");

        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(15).withMinute(30).withSecond(0).withNano(0);
        Cita cita = new Cita();
        cita.setId(9500L);
        cita.setCodigoReserva("RES-2026-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        cita.setCliente(cliente);
        cita.setTrabajador(trabajador);
        cita.setServicio(servicio);
        cita.setFechaHoraInicio(inicio);
        cita.setFechaHoraFin(inicio.plusMinutes(90));
        cita.setEstado(EstadoCita.PENDIENTE);
        cita.setPrecioFinal(new BigDecimal("45000"));
        cita.setNotasCliente("Prefiero tonos cálidos avellana y tengo el cabello sensible.");

        List<CorreoEnviadoDTO> generados = new ArrayList<>();
        // 1. Bienvenida al cliente
        generados.add(emailService.enviarBienvenida(uCliente));
        // 2. Comprobante de pago y reserva recibida al cliente
        generados.add(emailService.enviarComprobantePagoYReserva(cita));
        // 3. Aviso al trabajador de que agendaron con él
        generados.add(emailService.enviarAvisoNuevaCitaTrabajador(cita));
        // 4. Confirmación del trabajador hacia el cliente
        cita.setEstado(EstadoCita.CONFIRMADA);
        generados.add(emailService.enviarCitaConfirmada(cita));
        // 5. Recordatorio 24 horas antes
        generados.add(emailService.enviarRecordatorio(cita));
        // 6. Aviso de cancelación
        cita.setEstado(EstadoCita.CANCELADA);
        cita.setMotivoCancelacion("Reagendamiento solicitado por preferencia de horario.");
        generados.add(emailService.enviarCitaCancelada(cita));

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("codigoReserva", cita.getCodigoReserva());
        respuesta.put("correoCliente", correoCliente);
        respuesta.put("correoTrabajador", correoTrabajador);
        respuesta.put("smtpConfigurado", emailService.isSmtpConfigurado());
        respuesta.put("enviados", generados);
        return ResponseEntity.ok(respuesta);
    }
}
