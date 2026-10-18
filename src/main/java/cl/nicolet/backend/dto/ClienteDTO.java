package cl.nicolet.backend.dto;

import java.time.LocalDate;
import java.util.Objects;

public class ClienteDTO {
    private Long id;
    private UsuarioDTO usuario;
    private String telefono;
    private String rutDni;
    private LocalDate fechaNacimiento;
    private String notasPreferencias;
    private Boolean activo;

    public ClienteDTO() {
    }

    public ClienteDTO(Long id, UsuarioDTO usuario, String telefono, String rutDni,
                      LocalDate fechaNacimiento, String notasPreferencias, Boolean activo) {
        this.id = id;
        this.usuario = usuario;
        this.telefono = telefono;
        this.rutDni = rutDni;
        this.fechaNacimiento = fechaNacimiento;
        this.notasPreferencias = notasPreferencias;
        this.activo = activo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UsuarioDTO getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioDTO usuario) {
        this.usuario = usuario;
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

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClienteDTO that)) return false;
        return Objects.equals(id, that.id)
                && Objects.equals(usuario, that.usuario)
                && Objects.equals(telefono, that.telefono)
                && Objects.equals(rutDni, that.rutDni)
                && Objects.equals(fechaNacimiento, that.fechaNacimiento)
                && Objects.equals(notasPreferencias, that.notasPreferencias)
                && Objects.equals(activo, that.activo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, usuario, telefono, rutDni, fechaNacimiento, notasPreferencias, activo);
    }
}
