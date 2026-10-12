package cl.nicolet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bloque horario calculado con estado de disponibilidad")
public class BloqueHorarioDTO {

    @Schema(description = "Hora de inicio del bloque", example = "10:00:00")
    private LocalTime horaInicio;

    @Schema(description = "Hora estimada de término según duración del servicio", example = "10:45:00")
    private LocalTime horaFin;

    @Schema(description = "Indica si el bloque está libre para ser agendado", example = "true")
    private Boolean disponible;

    @Schema(description = "Motivo por el que no está disponible (null si está libre)", example = "OCUPADO")
    private String motivoNoDisponible;
}
