package cl.nicolet.backend.service;

import cl.nicolet.backend.dto.BloqueHorarioDTO;
import cl.nicolet.backend.exception.RecursoNoEncontradoException;
import cl.nicolet.backend.model.Cita;
import cl.nicolet.backend.model.EstadoCita;
import cl.nicolet.backend.model.HorarioDisponibilidad;
import cl.nicolet.backend.model.Servicio;
import cl.nicolet.backend.model.Trabajador;
import cl.nicolet.backend.repository.CitaRepository;
import cl.nicolet.backend.repository.HorarioDisponibilidadRepository;
import cl.nicolet.backend.repository.ServicioRepository;
import cl.nicolet.backend.repository.TrabajadorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DisponibilidadService {

    private static final Logger log = LoggerFactory.getLogger(DisponibilidadService.class);

    @Autowired
    private TrabajadorRepository trabajadorRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private HorarioDisponibilidadRepository horarioDisponibilidadRepository;

    @Autowired
    private CitaRepository citaRepository;

    public List<BloqueHorarioDTO> calcularDisponibilidad(Long trabajadorId, LocalDate fecha, Long servicioId) {
        log.info("Calculando disponibilidad para trabajadorId={}, fecha={}, servicioId={}", trabajadorId, fecha, servicioId);

        // 1. Validar Trabajador
        Trabajador trabajador = trabajadorRepository.findById(trabajadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Trabajador no encontrado: " + trabajadorId));
        if (!Boolean.TRUE.equals(trabajador.getActivo())) {
            throw new IllegalArgumentException("El trabajador se encuentra inactivo");
        }

        // 2. Determinar duración del servicio
        int duracionMinutos = 45;
        if (servicioId != null) {
            Servicio servicio = servicioRepository.findById(servicioId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado: " + servicioId));
            if (!Boolean.TRUE.equals(servicio.getActivo())) {
                throw new IllegalArgumentException("El servicio seleccionado se encuentra inactivo");
            }
            duracionMinutos = servicio.getDuracionMinutos();
        }

        // 3. Obtener jornada del profesional para el día de la semana (1 = Lunes, ..., 7 = Domingo)
        int diaSemana = fecha.getDayOfWeek().getValue();
        Optional<HorarioDisponibilidad> horarioOpt = horarioDisponibilidadRepository
                .findByTrabajadorIdAndDiaSemanaAndActivoTrue(trabajadorId, diaSemana);

        if (horarioOpt.isEmpty()) {
            log.info("El trabajador id={} no tiene turnos configurados para el día {}", trabajadorId, diaSemana);
            return Collections.emptyList();
        }

        HorarioDisponibilidad horario = horarioOpt.get();

        // 4. Obtener citas agendadas activas en esa fecha
        LocalDateTime inicioDia = fecha.atTime(horario.getHoraInicio());
        LocalDateTime finDia = fecha.atTime(horario.getHoraFin());
        List<Cita> citasDelDia = citaRepository.findByTrabajadorAndRangoFechas(trabajadorId, inicioDia, finDia)
                .stream()
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA)
                .collect(Collectors.toList());

        // 5. Generar bloques horarios
        List<BloqueHorarioDTO> bloques = new ArrayList<>();
        LocalTime cursor = horario.getHoraInicio();
        int pasoMinutos = 30; // Intervalo de 30 minutos para opciones de inicio
        LocalTime ahora = LocalTime.now();
        boolean esHoy = fecha.isEqual(LocalDate.now());

        while (!cursor.plusMinutes(duracionMinutos).isAfter(horario.getHoraFin())) {
            LocalTime slotFin = cursor.plusMinutes(duracionMinutos);
            boolean disponible = true;
            String motivo = null;

            // Validación A: Hora pasada si la consulta es para hoy
            if (esHoy && cursor.isBefore(ahora.plusMinutes(5))) {
                disponible = false;
                motivo = "HORA_PASADA";
            }

            // Validación B: Cruce con horario de descanso / colación
            if (disponible && horario.getHoraInicioDescanso() != null && horario.getHoraFinDescanso() != null) {
                boolean solapaDescanso = cursor.isBefore(horario.getHoraFinDescanso()) &&
                                         slotFin.isAfter(horario.getHoraInicioDescanso());
                if (solapaDescanso) {
                    disponible = false;
                    motivo = "DESCANSO";
                }
            }

            // Validación C: Solapamiento con citas existentes
            if (disponible) {
                LocalDateTime slotInicioDateTime = fecha.atTime(cursor);
                LocalDateTime slotFinDateTime = fecha.atTime(slotFin);

                boolean solapaCita = citasDelDia.stream().anyMatch(c ->
                        slotInicioDateTime.isBefore(c.getFechaHoraFin()) &&
                        slotFinDateTime.isAfter(c.getFechaHoraInicio())
                );

                if (solapaCita) {
                    disponible = false;
                    motivo = "OCUPADO";
                }
            }

            bloques.add(new BloqueHorarioDTO(cursor, slotFin, disponible, motivo));
            cursor = cursor.plusMinutes(pasoMinutos);
        }

        return bloques;
    }
}
