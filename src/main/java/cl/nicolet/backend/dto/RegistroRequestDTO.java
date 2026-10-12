package cl.nicolet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para registro de nuevos usuarios en el sistema")
public class RegistroRequestDTO {

    @Schema(description = "Nombre del usuario", example = "Valentina")
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @Schema(description = "Apellido paterno del usuario", example = "Pérez")
    @NotBlank(message = "El apellido paterno es obligatorio")
    private String apellidoP;

    @Schema(description = "Correo electrónico único", example = "valentina.perez@gmail.com")
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Formato de correo electrónico inválido")
    private String correo;

    @Schema(description = "Contraseña de la cuenta (máximo 72 caracteres)", example = "ClaveSegura123!")
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres")
    private String password;

    @Schema(description = "Teléfono de contacto opcional", example = "+56912345678")
    private String telefono;

    public void setCorreo(String correo) {
        this.correo = (correo != null) ? correo.trim().toLowerCase() : null;
    }
}
