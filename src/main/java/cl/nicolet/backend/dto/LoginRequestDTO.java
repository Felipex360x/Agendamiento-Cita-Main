package cl.nicolet.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

public class LoginRequestDTO {

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 100, message = "El correo electrónico no puede exceder los 100 caracteres")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 72, message = "La contraseña no puede exceder los 72 caracteres")
    private String password;

    public LoginRequestDTO() {
    }

    public LoginRequestDTO(String correo, String password) {
        this.setCorreo(correo);
        this.password = password;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoginRequestDTO that)) return false;
        return Objects.equals(correo, that.correo) && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(correo, password);
    }
}
