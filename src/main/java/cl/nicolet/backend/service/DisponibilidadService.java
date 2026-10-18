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

        Trabajador trabajador = trabajadorRepository.findById(trabajadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Trabajador no encontrado: " + trabajadorId));
        if (!Boolean.TRUE.equals(trabajador.getActivo())) {
            throw new IllegalArgumentException("El trabajador se encuentra inactivo");
        }

        int duracionMinutos = 45;
        if (servicioId != null) {
            Servicio servicio = servicioRepository.findById(servicioId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado: " + servicioId));
            if (!Boolean.TRUE.equals(servicio.getActivo())) {
                throw new IllegalArgumentException("El servicio seleccionado se encuentra inactivo");
            }
            duracionMinutos = servicio.getDuracionMinutos();
        }

        int diaSemana = fecha.getDayOfWeek().getValue();
        Optional<HorarioDisponibilidad> horarioOpt = horarioDisponibilidadRepository
                .findByTrabajadorIdAndDiaSemanaAndActivoTrue(trabajadorId, diaSemana);

        if (horarioOpt.isEmpty()) {
            log.info("El trabajador id={} no tiene turnos configurados para el día {}", trabajadorId, diaSemana);
            return Collections.emptyList();
        }

        HorarioDisponibilidad horario = horarioOpt.get();

        LocalDateTime inicioDia = fecha.atTime(horario.getHoraInicio());
        LocalDateTime finDia = fecha.atTime(horario.getHoraFin());
        List<Cita> citasDelDia = citaRepository.findByTrabajadorAndRangoFechas(trabajadorId, inicioDia, finDia)
                .stream()
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA)
                .collect(Collectors.toList());

        List<BloqueHorarioDTO> bloques = new ArrayList<>();
        LocalDateTime cursorDT = fecha.atTime(horario.getHoraInicio());
        LocalDateTime limiteFinDT = fecha.atTime(horario.getHoraFin());
        int pasoMinutos = 30;
        LocalTime ahora = LocalTime.now();
        boolean esHoy = fecha.isEqual(LocalDate.now());

        while (!cursorDT.plusMinutes(duracionMinutos).isAfter(limiteFinDT)) {
            LocalDateTime slotFinDateTime = cursorDT.plusMinutes(duracionMinutos);
            LocalTime cursor = cursorDT.toLocalTime();
            LocalTime slotFin = slotFinDateTime.toLocalTime();
            boolean disponible = true;
            String motivo = null;

            if (esHoy && cursor.isBefore(ahora.plusMinutes(5))) {
                disponible = false;
                motivo = "HORA_PASADA";
            }

            if (disponible && horario.getHoraInicioDescanso() != null && horario.getHoraFinDescanso() != null) {
                boolean solapaDescanso = cursor.isBefore(horario.getHoraFinDescanso()) &&
                                         slotFin.isAfter(horario.getHoraInicioDescanso());
                if (solapaDescanso) {
                    disponible = false;
                    motivo = "DESCANSO";
                }
            }

            if (disponible) {
                LocalDateTime slotInicioDateTime = cursorDT;

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
            cursorDT = cursorDT.plusMinutes(pasoMinutos);
        }

        return bloques;
    }
}
