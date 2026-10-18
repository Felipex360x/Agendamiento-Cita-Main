package cl.nicolet.backend.dto;

import java.util.Objects;

public class UsuarioDTO {

    private Long id;
    private String nombre;
    private String apellidoP;
    private String correo;
    private TipoUsuarioDTO tipoUsuario;

    public UsuarioDTO() {
    }

    public UsuarioDTO(Long id, String nombre, String apellidoP, String correo) {
        this.id = id;
        this.nombre = nombre;
        this.apellidoP = apellidoP;
        this.correo = correo;
        this.tipoUsuario = null;
    }

    public UsuarioDTO(Long id, String nombre, String apellidoP, String correo, TipoUsuarioDTO tipoUsuario) {
        this.id = id;
        this.nombre = nombre;
        this.apellidoP = apellidoP;
        this.correo = correo;
        this.tipoUsuario = tipoUsuario;
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

    public String getApellidoP() {
        return apellidoP;
    }

    public void setApellidoP(String apellidoP) {
        this.apellidoP = apellidoP;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public TipoUsuarioDTO getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(TipoUsuarioDTO tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioDTO that)) return false;
        return Objects.equals(id, that.id)
                && Objects.equals(nombre, that.nombre)
                && Objects.equals(apellidoP, that.apellidoP)
                && Objects.equals(correo, that.correo)
                && Objects.equals(tipoUsuario, that.tipoUsuario);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nombre, apellidoP, correo, tipoUsuario);
    }
}