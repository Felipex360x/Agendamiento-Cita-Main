package cl.nicolet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoUsuarioCreateDTO {

    @Schema(description = "Nombre o clave del rol", example = "CLIENTE")
    @NotBlank(message = "El nombre del tipo de usuario no puede estar vacío")
    private String nombre;

    @Schema(description = "Descripción del tipo de usuario", example = "Cliente que agenda citas")
    private String descripcion;

}
