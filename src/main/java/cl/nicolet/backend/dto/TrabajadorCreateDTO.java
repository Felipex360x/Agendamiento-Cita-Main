package cl.nicolet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrabajadorCreateDTO {

    @Schema(description = "ID del usuario asociado", example = "4")
    @NotNull(message = "El ID de usuario es obligatorio")
    private Long usuarioId;

    @Schema(description = "RUT o DNI", example = "18234567-8")
    private String rutDni;

    @Schema(description = "Teléfono de contacto", example = "+56987654321")
    private String telefono;

    @Schema(description = "Especialidad o cargo", example = "Especialista en Estilismo y Manicura")
    @NotBlank(message = "La especialidad o cargo es obligatorio")
    private String cargoEspecialidad;

    @Schema(description = "Biografía o presentación", example = "5 años de experiencia en estética")
    private String biografia;

    @Schema(description = "Porcentaje de comisión", example = "30.00")
    private BigDecimal comisionPorcentaje;

    @Schema(description = "IDs de servicios que atiende", example = "[1, 2]")
    private Set<Long> servicioIds;
}
