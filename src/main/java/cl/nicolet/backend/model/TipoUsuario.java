package cl.nicolet.backend.model;


import jakarta.persistence.Entity;
import  jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data 
@Entity 
//vinculamos la calse con la tabla de mysqls
@Table(name = "tipo_usuario")
@NoArgsConstructor 
@AllArgsConstructor
public class TipoUsuario {
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String descripcion;
    
}
