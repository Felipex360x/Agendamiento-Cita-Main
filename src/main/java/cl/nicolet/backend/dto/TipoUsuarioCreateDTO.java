package cl.nicolet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;

public class TipoUsuarioCreateDTO {

    @Schema(description = "Nombre o clave del rol", example = "CLIENTE")
    @NotBlank(message = "El nombre del tipo de usuario no puede estar vacío")
    private String nombre;

    @Schema(description = "Descripción del tipo de usuario", example = "Cliente que agenda citas")
    private String descripcion;

    public TipoUsuarioCreateDTO() {
    }

    public TipoUsuarioCreateDTO(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TipoUsuarioCreateDTO that)) return false;
        return Objects.equals(nombre, that.nombre) && Objects.equals(descripcion, that.descripcion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre, descripcion);
    }
}
