package cl.nicolet.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponseDTO {

    private String token;
    @Builder.Default
    private String tipoToken = "Bearer";
    private Long id;
    private String nombre;
    private String apellidoP;
    private String correo;
    private String rol;
}
