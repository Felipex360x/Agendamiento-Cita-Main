package cl.nicolet.backend.service;

import cl.nicolet.backend.dto.BloqueHorarioDTO;
import cl.nicolet.backend.model.Cita;
import cl.nicolet.backend.model.EstadoCita;
import cl.nicolet.backend.model.HorarioDisponibilidad;
import cl.nicolet.backend.model.Servicio;
import cl.nicolet.backend.model.Trabajador;
import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.repository.CitaRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceTest {

    @Mock
    private TrabajadorRepository trabajadorRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private HorarioDisponibilidadRepository horarioDisponibilidadRepository;

    @Mock
    private CitaRepository citaRepository;

    @InjectMocks
    private DisponibilidadService disponibilidadService;

    @Test
    @DisplayName("calcularDisponibilidad - debe retornar lista vacia si el trabajador no atiende ese dia")
    void debeRetornarVacioSiNoAtiendeEseDia() {
        Trabajador trabajador = new Trabajador();
        trabajador.setId(1L);
        trabajador.setActivo(true);
        trabajador.setUsuario(new Usuario());

        LocalDate domingo = LocalDate.now().plusDays(7);
        while (domingo.getDayOfWeek().getValue() != 7) {
            domingo = domingo.plusDays(1);
        }

        when(trabajadorRepository.findById(1L)).thenReturn(Optional.of(trabajador));
        when(horarioDisponibilidadRepository.findByTrabajadorIdAndDiaSemanaAndActivoTrue(1L, 7))
                .thenReturn(Optional.empty());

        List<BloqueHorarioDTO> resultado = disponibilidadService.calcularDisponibilidad(1L, domingo, null);

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("calcularDisponibilidad - debe lanzar excepcion si el trabajador esta inactivo")
    void debeLanzarExcepcionSiTrabajadorInactivo() {
        Trabajador trabajador = new Trabajador();
        trabajador.setId(1L);
        trabajador.setActivo(false);
        trabajador.setUsuario(new Usuario());

        when(trabajadorRepository.findById(1L)).thenReturn(Optional.of(trabajador));

        assertThrows(IllegalArgumentException.class, () ->
                disponibilidadService.calcularDisponibilidad(1L, LocalDate.now().plusDays(1), null));
    }

    @Test
    @DisplayName("calcularDisponibilidad - debe marcar bloques en descanso como DESCANSO")
    void debeMarcarDescansoComoNoDisponible() {
        Trabajador trabajador = new Trabajador();
        trabajador.setId(1L);
        trabajador.setActivo(true);
        trabajador.setUsuario(new Usuario());

        Servicio servicio = new Servicio(1L, "Corte", "Desc", 60, new BigDecimal("15000"), true);

        LocalDate fechaFutura = LocalDate.now().plusDays(2);
        int diaSemana = fechaFutura.getDayOfWeek().getValue();

        HorarioDisponibilidad horario = new HorarioDisponibilidad(
                1L, trabajador, diaSemana,
                LocalTime.of(9, 0), LocalTime.of(15, 0),
                LocalTime.of(13, 0), LocalTime.of(14, 0),
                true
        );

        when(trabajadorRepository.findById(1L)).thenReturn(Optional.of(trabajador));
        when(servicioRepository.findById(1L)).thenReturn(Optional.of(servicio));
        when(horarioDisponibilidadRepository.findByTrabajadorIdAndDiaSemanaAndActivoTrue(1L, diaSemana))
                .thenReturn(Optional.of(horario));
        when(citaRepository.findByTrabajadorAndRangoFechas(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        List<BloqueHorarioDTO> bloques = disponibilidadService.calcularDisponibilidad(1L, fechaFutura, 1L);

        assertFalse(bloques.isEmpty());

        BloqueHorarioDTO bloqueDescanso = bloques.stream()
                .filter(b -> b.getHoraInicio().equals(LocalTime.of(13, 0)))
                .findFirst()
                .orElse(null);

        assertNotNull(bloqueDescanso);
        assertFalse(bloqueDescanso.getDisponible());
        assertEquals("DESCANSO", bloqueDescanso.getMotivoNoDisponible());
    }

    @Test
    @DisplayName("calcularDisponibilidad - debe marcar bloques con citas agendadas como OCUPADO")
    void debeMarcarCitaExistenteComoOcupado() {
        Trabajador trabajador = new Trabajador();
        trabajador.setId(1L);
        trabajador.setActivo(true);
        trabajador.setUsuario(new Usuario());

        Servicio servicio = new Servicio(1L, "Manicura", "Desc", 60, new BigDecimal("20000"), true);

        LocalDate fechaFutura = LocalDate.now().plusDays(3);
        int diaSemana = fechaFutura.getDayOfWeek().getValue();

        HorarioDisponibilidad horario = new HorarioDisponibilidad(
                1L, trabajador, diaSemana,
                LocalTime.of(9, 0), LocalTime.of(18, 0),
                null, null,
                true
        );

        Cita cita = new Cita();
        cita.setId(50L);
        cita.setEstado(EstadoCita.CONFIRMADA);
        cita.setFechaHoraInicio(fechaFutura.atTime(10, 0));
        cita.setFechaHoraFin(fechaFutura.atTime(11, 0));

        when(trabajadorRepository.findById(1L)).thenReturn(Optional.of(trabajador));
        when(servicioRepository.findById(1L)).thenReturn(Optional.of(servicio));
        when(horarioDisponibilidadRepository.findByTrabajadorIdAndDiaSemanaAndActivoTrue(1L, diaSemana))
                .thenReturn(Optional.of(horario));
        when(citaRepository.findByTrabajadorAndRangoFechas(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(cita));

        List<BloqueHorarioDTO> bloques = disponibilidadService.calcularDisponibilidad(1L, fechaFutura, 1L);

        BloqueHorarioDTO bloqueOcupado = bloques.stream()
                .filter(b -> b.getHoraInicio().equals(LocalTime.of(10, 0)))
                .findFirst()
                .orElse(null);

        assertNotNull(bloqueOcupado);
        assertFalse(bloqueOcupado.getDisponible());
        assertEquals("OCUPADO", bloqueOcupado.getMotivoNoDisponible());

        BloqueHorarioDTO bloqueLibre = bloques.stream()
                .filter(b -> b.getHoraInicio().equals(LocalTime.of(9, 0)))
                .findFirst()
                .orElse(null);

        assertNotNull(bloqueLibre);
        assertTrue(bloqueLibre.getDisponible());
        assertNull(bloqueLibre.getMotivoNoDisponible());
    }
}
