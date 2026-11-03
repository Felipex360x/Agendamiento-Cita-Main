package cl.nicolet.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class ClientePerfilUpdateDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede exceder los 100 caracteres")
    private String apellidoP;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Ingresa un correo válido")
    @Size(max = 150, message = "El correo electrónico no puede exceder los 150 caracteres")
    private String correo;

    @Size(max = 20, message = "El teléfono no puede exceder los 20 caracteres")
    private String telefono;
    @Size(max = 20, message = "El RUT/DNI no puede exceder los 20 caracteres")
    private String rutDni;
    private LocalDate fechaNacimiento;
    @Size(max = 2000, message = "Las notas no pueden exceder los 2000 caracteres")
    private String notasPreferencias;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellidoP() { return apellidoP; }
    public void setApellidoP(String apellidoP) { this.apellidoP = apellidoP; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getRutDni() { return rutDni; }
    public void setRutDni(String rutDni) { this.rutDni = rutDni; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getNotasPreferencias() { return notasPreferencias; }
    public void setNotasPreferencias(String notasPreferencias) { this.notasPreferencias = notasPreferencias; }
}
