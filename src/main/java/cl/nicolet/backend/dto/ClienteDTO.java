package cl.nicolet.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteDTO {
    private Long id;
    private UsuarioDTO usuario;
    private String telefono;
    private String rutDni;
    private LocalDate fechaNacimiento;
    private String notasPreferencias;
    private Boolean activo;
}
