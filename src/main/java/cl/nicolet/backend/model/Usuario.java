package cl.nicolet.backend.model;

import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@Entity 
@Table (name = "usuario")
@NoArgsConstructor 
@AllArgsConstructor 
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

    public Usuario(Long id, String nombre, String apellidoP, String correo, String password) {
        this.id = id;
        this.nombre = nombre;
        this.apellidoP = apellidoP;
        this.correo = correo;
        this.password = password;
    }
}
