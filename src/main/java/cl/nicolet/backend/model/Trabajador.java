package cl.nicolet.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "trabajador")
public class Trabajador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "rut_dni", unique = true)
    private String rutDni;

    private String telefono;

    @Column(name = "cargo_especialidad", nullable = false, length = 100)
    private String cargoEspecialidad;

    @Column(columnDefinition = "TEXT")
    private String biografia;

    @Column(name = "comision_porcentaje", precision = 5, scale = 2)
    private BigDecimal comisionPorcentaje = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "trabajador_servicio",
        joinColumns = @JoinColumn(name = "trabajador_id"),
        inverseJoinColumns = @JoinColumn(name = "servicio_id")
    )
    private Set<Servicio> servicios = new HashSet<>();

    public Trabajador() {
    }

    public Trabajador(Long id, Usuario usuario, String rutDni, String telefono, String cargoEspecialidad,
                      String biografia, BigDecimal comisionPorcentaje, Boolean activo, Set<Servicio> servicios) {
        this.id = id;
        this.usuario = usuario;
        this.rutDni = rutDni;
        this.telefono = telefono;
        this.cargoEspecialidad = cargoEspecialidad;
        this.biografia = biografia;
        this.comisionPorcentaje = comisionPorcentaje;
        this.activo = activo;
        this.servicios = servicios;
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

    public String getRutDni() {
        return rutDni;
    }

    public void setRutDni(String rutDni) {
        this.rutDni = rutDni;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCargoEspecialidad() {
        return cargoEspecialidad;
    }

    public void setCargoEspecialidad(String cargoEspecialidad) {
        this.cargoEspecialidad = cargoEspecialidad;
    }

    public String getBiografia() {
        return biografia;
    }

    public void setBiografia(String biografia) {
        this.biografia = biografia;
    }

    public BigDecimal getComisionPorcentaje() {
        return comisionPorcentaje;
    }

    public void setComisionPorcentaje(BigDecimal comisionPorcentaje) {
        this.comisionPorcentaje = comisionPorcentaje;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Set<Servicio> getServicios() {
        return servicios;
    }

    public void setServicios(Set<Servicio> servicios) {
        this.servicios = servicios;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Trabajador that)) return false;
        return Objects.equals(id, that.id)
                && Objects.equals(usuario, that.usuario)
                && Objects.equals(rutDni, that.rutDni)
                && Objects.equals(telefono, that.telefono)
                && Objects.equals(cargoEspecialidad, that.cargoEspecialidad)
                && Objects.equals(biografia, that.biografia)
                && Objects.equals(comisionPorcentaje, that.comisionPorcentaje)
                && Objects.equals(activo, that.activo)
                && Objects.equals(servicios, that.servicios);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, usuario, rutDni, telefono, cargoEspecialidad, biografia, comisionPorcentaje, activo, servicios);
    }

    @Override
    public String toString() {
        return "Trabajador(id=" + id + ", usuario=" + usuario + ", cargoEspecialidad=" + cargoEspecialidad
                + ", activo=" + activo + ")";
    }
}
