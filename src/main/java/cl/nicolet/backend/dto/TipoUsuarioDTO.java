package cl.nicolet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

public class TipoUsuarioDTO {

    @Schema(description = "ID del tipo de usuario", example = "1")
    private Long id;

    @Schema(description = "Nombre o clave del rol", example = "ADMINISTRADOR")
    private String nombre;

    @Schema(description = "Descripción del rol", example = "Administrador con acceso total al sistema")
    private String descripcion;

    public TipoUsuarioDTO() {
    }

    public TipoUsuarioDTO(Long id, String nombre, String descripcion) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TipoUsuarioDTO that)) return false;
        return Objects.equals(id, that.id)
                && Objects.equals(nombre, that.nombre)
                && Objects.equals(descripcion, that.descripcion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nombre, descripcion);
    }
}
