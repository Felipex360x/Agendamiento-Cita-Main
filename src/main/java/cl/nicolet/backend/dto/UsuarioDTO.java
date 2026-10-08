package cl.nicolet.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioDTO {

    private Long id;
    private String nombre;
    private String apellidoP;
    private String correo;
    private String password;
    private TipoUsuarioDTO tipoUsuario;

    public UsuarioDTO(Long id, String nombre, String apellidoP, String correo, String password) {
        this.id = id;
        this.nombre = nombre;
        this.apellidoP = apellidoP;
        this.correo = correo;
        this.password = password;
        this.tipoUsuario = null;
    }

}