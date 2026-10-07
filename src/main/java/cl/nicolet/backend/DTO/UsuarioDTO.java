package cl.nicolet.backend_usuario.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioDTO {

private Long id;

/*apellido paterno */

private String nombre;

private String apellidoP;

/*nombre de usuario */

private String correo;

private String password;

}