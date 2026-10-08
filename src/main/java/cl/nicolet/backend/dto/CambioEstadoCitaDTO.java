package cl.nicolet.backend.dto;

import cl.nicolet.backend.model.EstadoCita;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CambioEstadoCitaDTO {

    @Schema(description = "Nuevo estado de la cita", example = "CONFIRMADA")
    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoCita nuevoEstado;

    @Schema(description = "Motivo de cancelación o notas del cambio", example = "Cancelado por el cliente con 24h de anticipación")
    private String motivoONotas;
}
