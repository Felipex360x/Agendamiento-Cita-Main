package cl.nicolet.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para ejecutar el flujo de correos de prueba. Los nombres no admiten saltos de línea
 * ni caracteres de control porque terminan en el asunto del correo.
 */
public class PruebaFlujoCorreosDTO {

    private static final String SIN_CONTROL = "^[^\\p{Cntrl}]*$";

    @NotBlank(message = "El correo del cliente es obligatorio")
    @Email(message = "El correo del cliente no tiene un formato válido")
    @Size(max = 150, message = "El correo del cliente no puede exceder los 150 caracteres")
    private String correoCliente;

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 100, message = "El nombre del cliente no puede exceder los 100 caracteres")
    @Pattern(regexp = SIN_CONTROL, message = "El nombre del cliente contiene caracteres no permitidos")
    private String nombreCliente;

    @Size(max = 100, message = "El apellido del cliente no puede exceder los 100 caracteres")
    @Pattern(regexp = SIN_CONTROL, message = "El apellido del cliente contiene caracteres no permitidos")
    private String apellidoCliente;

    @NotBlank(message = "El correo del profesional es obligatorio")
    @Email(message = "El correo del profesional no tiene un formato válido")
    @Size(max = 150, message = "El correo del profesional no puede exceder los 150 caracteres")
    private String correoTrabajador;

    @NotBlank(message = "El nombre del profesional es obligatorio")
    @Size(max = 100, message = "El nombre del profesional no puede exceder los 100 caracteres")
    @Pattern(regexp = SIN_CONTROL, message = "El nombre del profesional contiene caracteres no permitidos")
    private String nombreTrabajador;

    @Size(max = 100, message = "El apellido del profesional no puede exceder los 100 caracteres")
    @Pattern(regexp = SIN_CONTROL, message = "El apellido del profesional contiene caracteres no permitidos")
    private String apellidoTrabajador;

    public PruebaFlujoCorreosDTO() {
    }

    private static String limpiar(String valor) {
        return valor != null ? valor.trim() : null;
    }

    public String getCorreoCliente() {
        return correoCliente;
    }

    public void setCorreoCliente(String correoCliente) {
        this.correoCliente = limpiar(correoCliente);
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = limpiar(nombreCliente);
    }

    public String getApellidoCliente() {
        return apellidoCliente;
    }

    public void setApellidoCliente(String apellidoCliente) {
        this.apellidoCliente = limpiar(apellidoCliente);
    }

    public String getCorreoTrabajador() {
        return correoTrabajador;
    }

    public void setCorreoTrabajador(String correoTrabajador) {
        this.correoTrabajador = limpiar(correoTrabajador);
    }

    public String getNombreTrabajador() {
        return nombreTrabajador;
    }

    public void setNombreTrabajador(String nombreTrabajador) {
        this.nombreTrabajador = limpiar(nombreTrabajador);
    }

    public String getApellidoTrabajador() {
        return apellidoTrabajador;
    }

    public void setApellidoTrabajador(String apellidoTrabajador) {
        this.apellidoTrabajador = limpiar(apellidoTrabajador);
    }
}
