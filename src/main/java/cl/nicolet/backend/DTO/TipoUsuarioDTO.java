package cl.nicolet.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoUsuarioDTO {

    @Schema(description = "ID del tipo de usuario", example = "1")
    private Long id;

    @Schema(description = "Nombre o clave del rol", example = "ADMINISTRADOR")
    private String nombre;

    @Schema(description = "Descripción del rol", example = "Administrador con acceso total al sistema")
    private String descripcion;

}
