package cl.nicolet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioCreateDTO {

    @Schema(description = "Nombre del Usuario", example = "maximo")
    @NotBlank(message = "el nombre no puede estar vacio")
    private String nombre;

    @Schema(description = "apellido paterno del Usuario", example = "rojas")
    @NotBlank(message = "el Apellido Paterno no puede estar vacio")
    private String apellidoP;

    @Schema(description = "correo de usuario", example = "maixmo@gmail.com")
    @NotBlank(message = "el Correo  no puede estar vacio")
    private String correo;

    @Schema(description = "contraseña", example = "123124")
    @NotBlank(message = "la contraseña no puede estar vacia")
    private String password;

    @Schema(description = "ID del tipo de usuario asignado", example = "1")
    private Long tipoUsuarioId;

    public UsuarioCreateDTO(String nombre, String apellidoP, String correo, String password) {
        this.nombre = nombre;
        this.apellidoP = apellidoP;
        this.correo = correo;
        this.password = password;
        this.tipoUsuarioId = null;
    }

}