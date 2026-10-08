package cl.nicolet.backend.service;

import cl.nicolet.backend.dto.CambioEstadoCitaDTO;
import cl.nicolet.backend.dto.CitaCreateDTO;
import cl.nicolet.backend.dto.CitaDTO;
import cl.nicolet.backend.dto.ClienteDTO;
import cl.nicolet.backend.dto.ServicioDTO;
import cl.nicolet.backend.dto.TrabajadorDTO;
import cl.nicolet.backend.model.Cita;
import cl.nicolet.backend.model.Cliente;
import cl.nicolet.backend.model.EstadoCita;
import cl.nicolet.backend.model.HorarioDisponibilidad;
import cl.nicolet.backend.model.Servicio;
import cl.nicolet.backend.model.Trabajador;
import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.repository.CitaRepository;
import cl.nicolet.backend.repository.ClienteRepository;
import cl.nicolet.backend.repository.HorarioDisponibilidadRepository;
import cl.nicolet.backend.repository.ServicioRepository;
import cl.nicolet.backend.repository.TrabajadorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private TrabajadorRepository trabajadorRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private HorarioDisponibilidadRepository horarioDisponibilidadRepository;

    @Mock
    private ClienteService clienteService;

    @Mock
    private TrabajadorService trabajadorService;

    @Mock
    private ServicioService servicioService;

    @InjectMocks
    private CitaService citaService;

    @Test
    @DisplayName("agendar - debe crear cita exitosamente con estado CONFIRMADA y hora fin calculada")
    void debeAgendarCitaExitosamente() {
        Usuario uCliente = new Usuario(1L, "Martina", "Contreras", "m@c.cl", "123");
        Cliente cliente = new Cliente(1L, uCliente, "+569123", "12345678-9", null, null, true);

        Usuario uTrabajador = new Usuario(2L, "Camila", "Silva", "c@s.cl", "123");
        Trabajador trabajador = new Trabajador(1L, uTrabajador, "98765432-1", "+569876", "Estilista", null, BigDecimal.ZERO, true, Set.of());

        Servicio servicio = new Servicio(1L, "Corte", "Desc", 45, new BigDecimal("15000"), true);

        LocalDateTime inicio = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        CitaCreateDTO dto = new CitaCreateDTO(1L, 1L, 1L, inicio, "Notas");

        HorarioDisponibilidad horario = new HorarioDisponibilidad(
                1L, trabajador, inicio.getDayOfWeek().getValue(),
                LocalTime.of(9, 0), LocalTime.of(18, 0),
                LocalTime.of(13, 0), LocalTime.of(14, 0), true
        );

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(trabajadorRepository.findById(1L)).thenReturn(Optional.of(trabajador));
        when(servicioRepository.findById(1L)).thenReturn(Optional.of(servicio));
        when(horarioDisponibilidadRepository.findByTrabajadorIdAndDiaSemanaAndActivoTrue(1L, inicio.getDayOfWeek().getValue()))
                .thenReturn(Optional.of(horario));
        when(citaRepository.existeSolapamiento(eq(1L), eq(inicio), any(LocalDateTime.class))).thenReturn(false);

        Cita citaGuardada = new Cita();
        citaGuardada.setId(100L);
        citaGuardada.setCodigoReserva("RES-2026-TEST01");
        citaGuardada.setCliente(cliente);
        citaGuardada.setTrabajador(trabajador);
        citaGuardada.setServicio(servicio);
        citaGuardada.setFechaHoraInicio(inicio);
        citaGuardada.setFechaHoraFin(inicio.plusMinutes(45));
        citaGuardada.setEstado(EstadoCita.CONFIRMADA);
        citaGuardada.setPrecioFinal(new BigDecimal("15000"));

        when(citaRepository.save(any(Cita.class))).thenReturn(citaGuardada);
        when(clienteService.toDTO(cliente)).thenReturn(new ClienteDTO());
        when(trabajadorService.toDTO(trabajador)).thenReturn(new TrabajadorDTO());
        when(servicioService.toDTO(servicio)).thenReturn(new ServicioDTO());

        CitaDTO resultado = citaService.agendar(dto);

        assertNotNull(resultado);
        assertEquals(100L, resultado.getId());
        assertEquals("RES-2026-TEST01", resultado.getCodigoReserva());
        assertEquals(EstadoCita.CONFIRMADA, resultado.getEstado());
        verify(citaRepository, times(1)).save(any(Cita.class));
    }

    @Test
    @DisplayName("agendar - debe lanzar excepcion si existe solapamiento de horario")
    void debeLanzarExcepcionSiExisteSolapamiento() {
        Usuario uCliente = new Usuario(1L, "Martina", "Contreras", "m@c.cl", "123");
        Cliente cliente = new Cliente(1L, uCliente, "+569123", "12345678-9", null, null, true);

        Usuario uTrabajador = new Usuario(2L, "Camila", "Silva", "c@s.cl", "123");
        Trabajador trabajador = new Trabajador(1L, uTrabajador, "98765432-1", "+569876", "Estilista", null, BigDecimal.ZERO, true, Set.of());

        Servicio servicio = new Servicio(1L, "Corte", "Desc", 45, new BigDecimal("15000"), true);
        LocalDateTime inicio = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        CitaCreateDTO dto = new CitaCreateDTO(1L, 1L, 1L, inicio, null);

        HorarioDisponibilidad horario = new HorarioDisponibilidad(
                1L, trabajador, inicio.getDayOfWeek().getValue(),
                LocalTime.of(9, 0), LocalTime.of(18, 0), null, null, true
        );

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(trabajadorRepository.findById(1L)).thenReturn(Optional.of(trabajador));
        when(servicioRepository.findById(1L)).thenReturn(Optional.of(servicio));
        when(horarioDisponibilidadRepository.findByTrabajadorIdAndDiaSemanaAndActivoTrue(1L, inicio.getDayOfWeek().getValue()))
                .thenReturn(Optional.of(horario));
        when(citaRepository.existeSolapamiento(eq(1L), eq(inicio), any(LocalDateTime.class))).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> citaService.agendar(dto));
        assertTrue(ex.getMessage().contains("ya tiene una cita agendada"));
        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    @DisplayName("cambiarEstado - debe prohibir cancelar una cita ya completada")
    void debeProhibirCancelarCitaCompletada() {
        Cita citaCompletada = new Cita();
        citaCompletada.setId(50L);
        citaCompletada.setEstado(EstadoCita.COMPLETADA);

        when(citaRepository.findById(50L)).thenReturn(Optional.of(citaCompletada));

        CambioEstadoCitaDTO dto = new CambioEstadoCitaDTO(EstadoCita.CANCELADA, "Motivo test");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> citaService.cambiarEstado(50L, dto));
        assertTrue(ex.getMessage().contains("No se puede modificar el estado de una cita ya completada"));
    }
}
