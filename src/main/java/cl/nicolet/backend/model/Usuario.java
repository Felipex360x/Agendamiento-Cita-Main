package cl.nicolet.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String apellidoP;
    private String correo;
    private String password;

    // conexion con la tabla de tipo usuario
    @ManyToOne
    @JoinColumn(name = "tipo_usuario_id")
    private TipoUsuario tipoUsuario;

    public Usuario() {
    }

    public Usuario(Long id, String nombre, String apellidoP, String correo, String password) {
        this.id = id;
        this.nombre = nombre;
        this.apellidoP = apellidoP;
        this.correo = correo;
        this.password = password;
    }

    public Usuario(Long id, String nombre, String apellidoP, String correo, String password, TipoUsuario tipoUsuario) {
        this.id = id;
        this.nombre = nombre;
        this.apellidoP = apellidoP;
        this.correo = correo;
        this.password = password;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public TipoUsuario getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(TipoUsuario tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario that)) return false;
        return Objects.equals(id, that.id)
                && Objects.equals(nombre, that.nombre)
                && Objects.equals(apellidoP, that.apellidoP)
                && Objects.equals(correo, that.correo)
                && Objects.equals(password, that.password)
                && Objects.equals(tipoUsuario, that.tipoUsuario);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nombre, apellidoP, correo, password, tipoUsuario);
    }

    @Override
    public String toString() {
        return "Usuario(id=" + id + ", nombre=" + nombre + ", apellidoP=" + apellidoP
                + ", correo=" + correo + ", tipoUsuario=" + tipoUsuario + ")";
    }
}
