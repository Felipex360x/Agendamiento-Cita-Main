package cl.nicolet.backend.dto;

import java.time.LocalDateTime;

/**
 * Una fila del historial de pagos que ve administración. No incluye el token ni el session_id de Webpay:
 * son datos de un solo uso que no deben salir del servidor.
 */
public record PagoHistorialDTO(
        Long id,
        Long citaId,
        String codigoReserva,
        String estado,
        String estadoCita,
        Long monto,
        String clienteNombre,
        String clienteCorreo,
        String servicio,
        String profesional,
        LocalDateTime fechaCita,
        String ordenCompra,
        String codigoAutorizacion,
        String tipoPago,
        String ultimos4Digitos,
        String fechaTransaccion,
        Integer responseCode,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn) {
}
