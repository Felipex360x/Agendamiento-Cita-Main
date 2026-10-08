package cl.nicolet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteCreateDTO {

    @Schema(description = "ID del usuario asociado", example = "2")
    @NotNull(message = "El ID de usuario es obligatorio")
    private Long usuarioId;

    @Schema(description = "Teléfono de contacto", example = "+56912345678")
    private String telefono;

    @Schema(description = "RUT o DNI del cliente", example = "19876543-2")
    private String rutDni;

    @Schema(description = "Fecha de nacimiento", example = "1998-05-14")
    private LocalDate fechaNacimiento;

    @Schema(description = "Notas sobre preferencias, alergias, etc.", example = "Prefiere tonos neutros")
    private String notasPreferencias;
}
