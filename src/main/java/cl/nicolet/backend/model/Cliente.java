package cl.nicolet.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    private String telefono;

    @Column(name = "rut_dni", unique = true)
    private String rutDni;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "notas_preferencias", columnDefinition = "TEXT")
    private String notasPreferencias;

    @Column(nullable = false)
    private Boolean activo = true;

    public Cliente() {
    }

    public Cliente(Long id, Usuario usuario, String telefono, String rutDni, LocalDate fechaNacimiento, String notasPreferencias, Boolean activo) {
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

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
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
        if (!(o instanceof Cliente that)) return false;
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

    @Override
    public String toString() {
        return "Cliente(id=" + id + ", usuario=" + usuario + ", telefono=" + telefono
                + ", rutDni=" + rutDni + ", activo=" + activo + ")";
    }
}
