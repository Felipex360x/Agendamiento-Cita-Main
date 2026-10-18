package cl.nicolet.backend.dto;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

public class TrabajadorDTO {
    private Long id;
    private UsuarioDTO usuario;
    private String rutDni;
    private String telefono;
    private String cargoEspecialidad;
    private String biografia;
    private BigDecimal comisionPorcentaje;
    private Boolean activo;
    private Set<ServicioDTO> servicios;

    public TrabajadorDTO() {
    }

    public TrabajadorDTO(Long id, UsuarioDTO usuario, String rutDni, String telefono, String cargoEspecialidad,
                         String biografia, BigDecimal comisionPorcentaje, Boolean activo, Set<ServicioDTO> servicios) {
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

    public UsuarioDTO getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioDTO usuario) {
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

    public Set<ServicioDTO> getServicios() {
        return servicios;
    }

    public void setServicios(Set<ServicioDTO> servicios) {
        this.servicios = servicios;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrabajadorDTO that)) return false;
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
}
