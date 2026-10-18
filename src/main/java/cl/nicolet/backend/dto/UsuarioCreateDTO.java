package cl.nicolet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

public class UsuarioCreateDTO {

    @Schema(description = "Nombre del Usuario", example = "maximo")
    @NotBlank(message = "el nombre no puede estar vacio")
    private String nombre;

    @Schema(description = "apellido paterno del Usuario", example = "rojas")
    @NotBlank(message = "el Apellido Paterno no puede estar vacio")
    private String apellidoP;

    @Schema(description = "correo de usuario", example = "maximo@gmail.com")
    @NotBlank(message = "el Correo no puede estar vacio")
    @Email(message = "Formato de correo electrónico inválido")
    private String correo;

    @Schema(description = "contraseña", example = "123124")
    @Size(max = 72, message = "la contraseña no puede exceder los 72 caracteres")
    private String password;

    @Schema(description = "ID del tipo de usuario asignado", example = "1")
    private Long tipoUsuarioId;

    public UsuarioCreateDTO() {
    }

    public UsuarioCreateDTO(String nombre, String apellidoP, String correo, String password) {
        this.nombre = nombre;
        this.apellidoP = apellidoP;
        this.setCorreo(correo);
        this.password = password;
        this.tipoUsuarioId = null;
    }

    public UsuarioCreateDTO(String nombre, String apellidoP, String correo, String password, Long tipoUsuarioId) {
        this.nombre = nombre;
        this.apellidoP = apellidoP;
        this.setCorreo(correo);
        this.password = password;
        this.tipoUsuarioId = tipoUsuarioId;
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

    public Long getTipoUsuarioId() {
        return tipoUsuarioId;
    }

    public void setTipoUsuarioId(Long tipoUsuarioId) {
        this.tipoUsuarioId = tipoUsuarioId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioCreateDTO that)) return false;
        return Objects.equals(nombre, that.nombre)
                && Objects.equals(apellidoP, that.apellidoP)
                && Objects.equals(correo, that.correo)
                && Objects.equals(password, that.password)
                && Objects.equals(tipoUsuarioId, that.tipoUsuarioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre, apellidoP, correo, password, tipoUsuarioId);
    }
}