package cl.nicolet.backend.dto;

import java.math.BigDecimal;
import java.util.Objects;

public class ServicioDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private Integer duracionMinutos;
    private BigDecimal precio;
    private Boolean activo;

    public ServicioDTO() {
    }

    public ServicioDTO(Long id, String nombre, String descripcion, Integer duracionMinutos, BigDecimal precio, Boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.duracionMinutos = duracionMinutos;
        this.precio = precio;
        this.activo = activo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(Integer duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
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
        if (!(o instanceof ServicioDTO that)) return false;
        return Objects.equals(id, that.id)
                && Objects.equals(nombre, that.nombre)
                && Objects.equals(descripcion, that.descripcion)
                && Objects.equals(duracionMinutos, that.duracionMinutos)
                && Objects.equals(precio, that.precio)
                && Objects.equals(activo, that.activo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nombre, descripcion, duracionMinutos, precio, activo);
    }
}
