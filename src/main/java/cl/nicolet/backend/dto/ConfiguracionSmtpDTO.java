package cl.nicolet.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credenciales del remitente SMTP. El servidor y el puerto no se aceptan desde la API:
 * solo se definen por configuración (spring.mail.host / spring.mail.port), para que nadie
 * pueda redirigir el correo del sistema ni hacer que el backend se conecte a otro equipo.
 */
public class ConfiguracionSmtpDTO {

    @NotBlank(message = "El correo remitente es obligatorio")
    @Email(message = "El correo remitente no tiene un formato válido")
    @Size(max = 150, message = "El correo remitente no puede exceder los 150 caracteres")
    private String remitente;

    @Size(max = 64, message = "La contraseña de aplicación no puede exceder los 64 caracteres")
    private String appPassword;

    public ConfiguracionSmtpDTO() {
    }

    public ConfiguracionSmtpDTO(String remitente, String appPassword) {
        this.setRemitente(remitente);
        this.appPassword = appPassword;
    }

    public String getRemitente() {
        return remitente;
    }

    public void setRemitente(String remitente) {
        this.remitente = (remitente != null) ? remitente.trim().toLowerCase() : null;
    }

    public String getAppPassword() {
        return appPassword;
    }

    public void setAppPassword(String appPassword) {
        this.appPassword = appPassword;
    }
}
