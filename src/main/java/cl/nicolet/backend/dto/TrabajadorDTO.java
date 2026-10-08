package cl.nicolet.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrabajadorDTO {
    private Long id;
    private UsuarioDTO usuario;
    private String rutDni;
    private String telefono;
    private String cargoEspecialidad;
    private String biografia;
    private BigDecimal comisionPorcentaje;
    private Boolean activo;
    private Set<ServicioDTO> servicios;
}
