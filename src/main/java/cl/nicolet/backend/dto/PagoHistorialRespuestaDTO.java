package cl.nicolet.backend.dto;

import java.util.List;

/** Historial de pagos, del más nuevo al más antiguo. {@code truncado} indica que hay más pagos que los mostrados. */
public record PagoHistorialRespuestaDTO(List<PagoHistorialDTO> pagos, boolean truncado) {
}
