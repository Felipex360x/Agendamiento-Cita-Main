package cl.nicolet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitaCreateDTO {

    @Schema(description = "ID del cliente que solicita la cita", example = "1")
    @NotNull(message = "El clienteId es obligatorio")
    private Long clienteId;

    @Schema(description = "ID del trabajador seleccionado", example = "1")
    @NotNull(message = "El trabajadorId es obligatorio")
    private Long trabajadorId;

    @Schema(description = "ID del servicio a agendar", example = "2")
    @NotNull(message = "El servicioId es obligatorio")
    private Long servicioId;

    @Schema(description = "Fecha y hora de inicio de la cita", example = "2026-10-15T10:00:00")
    @NotNull(message = "La fecha y hora de inicio es obligatoria")
    private LocalDateTime fechaHoraInicio;

    @Schema(description = "Notas adicionales ingresadas por el cliente", example = "Primera sesión")
    private String notasCliente;
}
