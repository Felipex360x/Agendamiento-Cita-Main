package cl.nicolet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Objects;

public class ServicioCreateDTO {

    @Schema(description = "Nombre del servicio", example = "Corte de Cabello y Peinado")
    @NotBlank(message = "El nombre del servicio no puede estar vacío")
    private String nombre;

    @Schema(description = "Descripción detallada", example = "Lavado, corte estilizado y secado profesional")
    private String descripcion;

    @Schema(description = "Duración estimada en minutos", example = "45")
    @NotNull(message = "La duración en minutos es obligatoria")
    @Min(value = 10, message = "La duración mínima es de 10 minutos")
    private Integer duracionMinutos;

    @Schema(description = "Precio del servicio", example = "18000.00")
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    private BigDecimal precio;

    public ServicioCreateDTO() {
    }

    public ServicioCreateDTO(String nombre, String descripcion, Integer duracionMinutos, BigDecimal precio) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.duracionMinutos = duracionMinutos;
        this.precio = precio;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServicioCreateDTO that)) return false;
        return Objects.equals(nombre, that.nombre)
                && Objects.equals(descripcion, that.descripcion)
                && Objects.equals(duracionMinutos, that.duracionMinutos)
                && Objects.equals(precio, that.precio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre, descripcion, duracionMinutos, precio);
    }
}
