package cl.nicolet.backend.dto;

/**
 * Datos para llevar al cliente al formulario de Webpay: el navegador debe enviar por POST
 * el campo {@code token_ws} (con el valor de {@code token}) a la dirección {@code url}.
 */
public record PagoInicioDTO(Long citaId, String codigoReserva, String token, String url, Long monto) {
}
