package cl.nicolet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

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

    public RegistroRequestDTO() {
    }

    public RegistroRequestDTO(String nombre, String apellidoP, String correo, String password, String telefono) {
        this.nombre = nombre;
        this.apellidoP = apellidoP;
        this.setCorreo(correo);
        this.password = password;
        this.telefono = telefono;
    }

    public static RegistroRequestDTOBuilder builder() {
        return new RegistroRequestDTOBuilder();
    }

    public static class RegistroRequestDTOBuilder {
        private String nombre;
        private String apellidoP;
        private String correo;
        private String password;
        private String telefono;

        public RegistroRequestDTOBuilder nombre(String nombre) {
            this.nombre = nombre;
            return this;
        }

        public RegistroRequestDTOBuilder apellidoP(String apellidoP) {
            this.apellidoP = apellidoP;
            return this;
        }

        public RegistroRequestDTOBuilder correo(String correo) {
            this.correo = correo;
            return this;
        }

        public RegistroRequestDTOBuilder password(String password) {
            this.password = password;
            return this;
        }

        public RegistroRequestDTOBuilder telefono(String telefono) {
            this.telefono = telefono;
            return this;
        }

        public RegistroRequestDTO build() {
            return new RegistroRequestDTO(nombre, apellidoP, correo, password, telefono);
        }
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidoP() {
        return apellidoP;
    }

    public void setApellidoP(String apellidoP) {
        this.apellidoP = apellidoP;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = (correo != null) ? correo.trim().toLowerCase() : null;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegistroRequestDTO that)) return false;
        return Objects.equals(nombre, that.nombre)
                && Objects.equals(apellidoP, that.apellidoP)
                && Objects.equals(correo, that.correo)
                && Objects.equals(password, that.password)
                && Objects.equals(telefono, that.telefono);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre, apellidoP, correo, password, telefono);
    }
}
