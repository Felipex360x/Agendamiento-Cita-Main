package cl.nicolet.backend.dto;

import cl.nicolet.backend.model.EstadoCita;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitaDTO {
    private Long id;
    private String codigoReserva;
    private ClienteDTO cliente;
    private TrabajadorDTO trabajador;
    private ServicioDTO servicio;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private EstadoCita estado;
    private BigDecimal precioFinal;
    private String notasCliente;
    private String notasTrabajador;
    private String motivoCancelacion;
}
