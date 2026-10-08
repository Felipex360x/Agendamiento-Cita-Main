package cl.nicolet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicioCreateDTO {

    @Schema(description = "Nombre del servicio", example = "Corte de Cabello y Peinado")
    @NotBlank(message = "El nombre del servicio no puede estar vacío")
    private String nombre;

    @Schema(description = "Descripción detallada", example = "Lavado, corte estilizado y secado profesional")
    private String descripcion;

    @Schema(description = "Duración estimada en minutos", example = "45")
    @NotNull(message = "La duración en minutos es obligatoria")
    @Min(value = 10, message = "La duración mínima es de 10 minutos")
    private Integer duracionMinutos;

    @Schema(description = "Precio del servicio", example = "18000.00")
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    private BigDecimal precio;
}
