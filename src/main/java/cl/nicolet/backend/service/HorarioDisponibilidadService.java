package cl.nicolet.backend.service;

import cl.nicolet.backend.dto.HorarioDisponibilidadDTO;
import cl.nicolet.backend.exception.RecursoNoEncontradoException;
import cl.nicolet.backend.model.HorarioDisponibilidad;
import cl.nicolet.backend.model.Trabajador;
import cl.nicolet.backend.repository.HorarioDisponibilidadRepository;
import cl.nicolet.backend.repository.TrabajadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class HorarioDisponibilidadService {
    private static final LocalTime DEFAULT_START = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_END = LocalTime.of(18, 0);

    private final HorarioDisponibilidadRepository horarioRepository;
    private final TrabajadorRepository trabajadorRepository;

    public HorarioDisponibilidadService(HorarioDisponibilidadRepository horarioRepository,
                                        TrabajadorRepository trabajadorRepository) {
        this.horarioRepository = horarioRepository;
        this.trabajadorRepository = trabajadorRepository;
    }

    @Transactional(readOnly = true)
    public List<HorarioDisponibilidadDTO> listar(Long trabajadorId) {
        trabajadorRepository.findById(trabajadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Trabajador no encontrado: " + trabajadorId));
        Map<Integer, HorarioDisponibilidad> existentes = horarioRepository.findByTrabajadorId(trabajadorId).stream()
                .collect(Collectors.toMap(HorarioDisponibilidad::getDiaSemana, horario -> horario, (first, ignored) -> first));
        List<HorarioDisponibilidadDTO> horarios = new ArrayList<>();
        for (int dia = 1; dia <= 7; dia++) {
            HorarioDisponibilidad horario = existentes.get(dia);
            horarios.add(horario == null
                    ? new HorarioDisponibilidadDTO(dia, DEFAULT_START, DEFAULT_END, null, null, false)
                    : toDTO(horario));
        }
        return horarios;
    }

    public List<HorarioDisponibilidadDTO> actualizar(Long trabajadorId, List<HorarioDisponibilidadDTO> horariosDTO) {
        Trabajador trabajador = trabajadorRepository.findById(trabajadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Trabajador no encontrado: " + trabajadorId));
        validar(horariosDTO);

        Map<Integer, HorarioDisponibilidad> existentes = horarioRepository.findByTrabajadorId(trabajadorId).stream()
                .collect(Collectors.toMap(HorarioDisponibilidad::getDiaSemana, horario -> horario, (first, ignored) -> first));
        List<HorarioDisponibilidad> guardar = new ArrayList<>();
        for (HorarioDisponibilidadDTO dto : horariosDTO) {
            HorarioDisponibilidad horario = existentes.getOrDefault(dto.getDiaSemana(), new HorarioDisponibilidad());
            horario.setTrabajador(trabajador);
            horario.setDiaSemana(dto.getDiaSemana());
            horario.setHoraInicio(dto.getHoraInicio());
            horario.setHoraFin(dto.getHoraFin());
            horario.setHoraInicioDescanso(dto.getHoraInicioDescanso());
            horario.setHoraFinDescanso(dto.getHoraFinDescanso());
            horario.setActivo(Boolean.TRUE.equals(dto.getActivo()));
            guardar.add(horario);
        }
        return horarioRepository.saveAll(guardar).stream().map(this::toDTO).toList();
    }

    private void validar(List<HorarioDisponibilidadDTO> horarios) {
        if (horarios == null || horarios.size() != 7) {
            throw new IllegalArgumentException("Debes enviar un horario para cada día de la semana");
        }
        Set<Integer> dias = new HashSet<>();
        for (HorarioDisponibilidadDTO horario : horarios) {
            Integer dia = horario.getDiaSemana();
            if (dia == null || dia < 1 || dia > 7 || !dias.add(dia)) {
                throw new IllegalArgumentException("Los días deben estar entre lunes (1) y domingo (7), sin repetir");
            }
            if (horario.getHoraInicio() == null || horario.getHoraFin() == null
                    || !horario.getHoraInicio().isBefore(horario.getHoraFin())) {
                throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de término");
            }
            boolean tieneInicioDescanso = horario.getHoraInicioDescanso() != null;
            boolean tieneFinDescanso = horario.getHoraFinDescanso() != null;
            if (tieneInicioDescanso != tieneFinDescanso) {
                throw new IllegalArgumentException("Completa ambas horas de colación o deja ambas vacías");
            }
            if (tieneInicioDescanso && (!horario.getHoraInicioDescanso().isBefore(horario.getHoraFinDescanso())
                    || horario.getHoraInicioDescanso().isBefore(horario.getHoraInicio())
                    || horario.getHoraFinDescanso().isAfter(horario.getHoraFin()))) {
                throw new IllegalArgumentException("La colación debe estar dentro de la jornada laboral");
            }
        }
    }

    private HorarioDisponibilidadDTO toDTO(HorarioDisponibilidad horario) {
        return new HorarioDisponibilidadDTO(horario.getDiaSemana(), horario.getHoraInicio(), horario.getHoraFin(),
                horario.getHoraInicioDescanso(), horario.getHoraFinDescanso(), horario.getActivo());
    }
}
