package cl.nicolet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

public class TrabajadorCreateDTO {

    @Schema(description = "ID del usuario asociado", example = "4")
    @NotNull(message = "El ID de usuario es obligatorio")
    private Long usuarioId;

    @Schema(description = "RUT o DNI", example = "18234567-8")
    private String rutDni;

    @Schema(description = "Teléfono de contacto", example = "+56987654321")
    private String telefono;

    @Schema(description = "Especialidad o cargo", example = "Especialista en Estilismo y Manicura")
    @NotBlank(message = "La especialidad o cargo es obligatorio")
    private String cargoEspecialidad;

    @Schema(description = "Biografía o presentación", example = "5 años de experiencia en estética")
    private String biografia;

    @Schema(description = "Porcentaje de comisión", example = "30.00")
    private BigDecimal comisionPorcentaje;

    @Schema(description = "IDs de servicios que atiende", example = "[1, 2]")
    private Set<Long> servicioIds;

    public TrabajadorCreateDTO() {
    }

    public TrabajadorCreateDTO(Long usuarioId, String rutDni, String telefono, String cargoEspecialidad,
                               String biografia, BigDecimal comisionPorcentaje, Set<Long> servicioIds) {
        this.usuarioId = usuarioId;
        this.rutDni = rutDni;
        this.telefono = telefono;
        this.cargoEspecialidad = cargoEspecialidad;
        this.biografia = biografia;
        this.comisionPorcentaje = comisionPorcentaje;
        this.servicioIds = servicioIds;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
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

    public Set<Long> getServicioIds() {
        return servicioIds;
    }

    public void setServicioIds(Set<Long> servicioIds) {
        this.servicioIds = servicioIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrabajadorCreateDTO that)) return false;
        return Objects.equals(usuarioId, that.usuarioId)
                && Objects.equals(rutDni, that.rutDni)
                && Objects.equals(telefono, that.telefono)
                && Objects.equals(cargoEspecialidad, that.cargoEspecialidad)
                && Objects.equals(biografia, that.biografia)
                && Objects.equals(comisionPorcentaje, that.comisionPorcentaje)
                && Objects.equals(servicioIds, that.servicioIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, rutDni, telefono, cargoEspecialidad, biografia, comisionPorcentaje, servicioIds);
    }
}
