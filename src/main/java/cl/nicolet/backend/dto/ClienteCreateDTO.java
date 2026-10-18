package cl.nicolet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Objects;

public class ClienteCreateDTO {

    @Schema(description = "ID del usuario asociado", example = "2")
    @NotNull(message = "El ID de usuario es obligatorio")
    private Long usuarioId;

    @Schema(description = "Teléfono de contacto", example = "+56912345678")
    private String telefono;

    @Schema(description = "RUT o DNI del cliente", example = "19876543-2")
    private String rutDni;

    @Schema(description = "Fecha de nacimiento", example = "1998-05-14")
    private LocalDate fechaNacimiento;

    @Schema(description = "Notas sobre preferencias, alergias, etc.", example = "Prefiere tonos neutros")
    private String notasPreferencias;

    public ClienteCreateDTO() {
    }

    public ClienteCreateDTO(Long usuarioId, String telefono, String rutDni, LocalDate fechaNacimiento, String notasPreferencias) {
        this.usuarioId = usuarioId;
        this.telefono = telefono;
        this.rutDni = rutDni;
        this.fechaNacimiento = fechaNacimiento;
        this.notasPreferencias = notasPreferencias;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getRutDni() {
        return rutDni;
    }

    public void setRutDni(String rutDni) {
        this.rutDni = rutDni;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getNotasPreferencias() {
        return notasPreferencias;
    }

    public void setNotasPreferencias(String notasPreferencias) {
        this.notasPreferencias = notasPreferencias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClienteCreateDTO that)) return false;
        return Objects.equals(usuarioId, that.usuarioId)
                && Objects.equals(telefono, that.telefono)
                && Objects.equals(rutDni, that.rutDni)
                && Objects.equals(fechaNacimiento, that.fechaNacimiento)
                && Objects.equals(notasPreferencias, that.notasPreferencias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, telefono, rutDni, fechaNacimiento, notasPreferencias);
    }
}
