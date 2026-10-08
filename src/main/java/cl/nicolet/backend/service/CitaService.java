package cl.nicolet.backend.service;

import cl.nicolet.backend.dto.CambioEstadoCitaDTO;
import cl.nicolet.backend.dto.CitaCreateDTO;
import cl.nicolet.backend.dto.CitaDTO;
import cl.nicolet.backend.exception.RecursoNoEncontradoException;
import cl.nicolet.backend.model.Cita;
import cl.nicolet.backend.model.Cliente;
import cl.nicolet.backend.model.EstadoCita;
import cl.nicolet.backend.model.HorarioDisponibilidad;
import cl.nicolet.backend.model.Servicio;
import cl.nicolet.backend.model.Trabajador;
import cl.nicolet.backend.repository.CitaRepository;
import cl.nicolet.backend.repository.ClienteRepository;
import cl.nicolet.backend.repository.HorarioDisponibilidadRepository;
import cl.nicolet.backend.repository.ServicioRepository;
import cl.nicolet.backend.repository.TrabajadorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CitaService {

    private static final Logger log = LoggerFactory.getLogger(CitaService.class);

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TrabajadorRepository trabajadorRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private HorarioDisponibilidadRepository horarioDisponibilidadRepository;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private TrabajadorService trabajadorService;

    @Autowired
    private ServicioService servicioService;

    public List<CitaDTO> findAll() {
        log.info("Consultando todas las citas");
        return citaRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public CitaDTO findById(Long id) {
        log.info("Buscando cita con id={}", id);
        Cita c = citaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada: " + id));
        return toDTO(c);
    }

    public CitaDTO findByCodigo(String codigoReserva) {
        log.info("Buscando cita por código={}", codigoReserva);
        Cita c = citaRepository.findByCodigoReserva(codigoReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada con código: " + codigoReserva));
        return toDTO(c);
    }

    public List<CitaDTO> findByTrabajador(Long trabajadorId) {
        log.info("Consultando citas del trabajador={}", trabajadorId);
        return citaRepository.findByTrabajadorId(trabajadorId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<CitaDTO> findByCliente(Long clienteId) {
        log.info("Consultando citas del cliente={}", clienteId);
        return citaRepository.findByClienteId(clienteId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public CitaDTO agendar(CitaCreateDTO dto) {
        log.info("Procesando agendamiento de cita para clienteId={}, trabajadorId={}, servicioId={}",
                dto.getClienteId(), dto.getTrabajadorId(), dto.getServicioId());

        // 1. Validar Cliente
        Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: " + dto.getClienteId()));
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new IllegalArgumentException("El cliente se encuentra inactivo");
        }

        // 2. Validar Trabajador
        Trabajador trabajador = trabajadorRepository.findById(dto.getTrabajadorId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Trabajador no encontrado: " + dto.getTrabajadorId()));
        if (!Boolean.TRUE.equals(trabajador.getActivo())) {
            throw new IllegalArgumentException("El trabajador se encuentra inactivo");
        }

        // 3. Validar Servicio
        Servicio servicio = servicioRepository.findById(dto.getServicioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado: " + dto.getServicioId()));
        if (!Boolean.TRUE.equals(servicio.getActivo())) {
            throw new IllegalArgumentException("El servicio seleccionado no está activo actualmente");
        }

        // 4. Validar Fecha futura (con 2 min de tolerancia para el llenado del formulario)
        LocalDateTime inicio = dto.getFechaHoraInicio();
        if (inicio.isBefore(LocalDateTime.now().minusMinutes(2))) {
            throw new IllegalArgumentException("La fecha y hora de agendamiento debe ser posterior al momento actual");
        }

        // 5. Cálculo automático de fin según duración del servicio
        LocalDateTime fin = inicio.plusMinutes(servicio.getDuracionMinutos());

        // 6. Validar contra jornada laboral del profesional
        validarHorarioLaboral(trabajador.getId(), inicio, fin);

        // 7. Validar Antisolapamiento de citas
        if (citaRepository.existeSolapamiento(trabajador.getId(), inicio, fin)) {
            throw new IllegalArgumentException("El profesional ya tiene una cita agendada en el horario solicitado");
        }

        // 8. Construir y guardar la cita
        Cita cita = new Cita();
        cita.setCodigoReserva(generarCodigoReserva());
        cita.setCliente(cliente);
        cita.setTrabajador(trabajador);
        cita.setServicio(servicio);
        cita.setFechaHoraInicio(inicio);
        cita.setFechaHoraFin(fin);
        cita.setEstado(EstadoCita.CONFIRMADA);
        cita.setPrecioFinal(servicio.getPrecio());
        cita.setNotasCliente(dto.getNotasCliente());

        Cita guardada = citaRepository.save(cita);
        log.info("Cita agendada exitosamente con código={}", guardada.getCodigoReserva());

        return toDTO(guardada);
    }

    public CitaDTO cambiarEstado(Long id, CambioEstadoCitaDTO dto) {
        log.info("Cambiando estado de cita id={} a {}", id, dto.getNuevoEstado());
        Cita c = citaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada: " + id));

        if (c.getEstado() == EstadoCita.COMPLETADA) {
            throw new IllegalArgumentException("No se puede modificar el estado de una cita ya completada");
        }

        if (dto.getNuevoEstado() == EstadoCita.CANCELADA) {
            c.setMotivoCancelacion(dto.getMotivoONotas());
        }

        c.setEstado(dto.getNuevoEstado());
        return toDTO(citaRepository.save(c));
    }

    private void validarHorarioLaboral(Long trabajadorId, LocalDateTime inicio, LocalDateTime fin) {
        int diaSemana = inicio.getDayOfWeek().getValue(); // 1 = Lunes, ..., 7 = Domingo
        Optional<HorarioDisponibilidad> horarioOpt = horarioDisponibilidadRepository
                .findByTrabajadorIdAndDiaSemanaAndActivoTrue(trabajadorId, diaSemana);

        if (horarioOpt.isEmpty()) {
            throw new IllegalArgumentException("El profesional no tiene turnos de atención disponibles el día seleccionado");
        }

        HorarioDisponibilidad horario = horarioOpt.get();
        LocalTime horaInicioCita = inicio.toLocalTime();
        LocalTime horaFinCita = fin.toLocalTime();

        if (horaInicioCita.isBefore(horario.getHoraInicio()) || horaFinCita.isAfter(horario.getHoraFin())) {
            throw new IllegalArgumentException(String.format(
                    "El horario solicitado (%s - %s) está fuera de la jornada de atención del profesional (%s - %s)",
                    horaInicioCita, horaFinCita, horario.getHoraInicio(), horario.getHoraFin()
            ));
        }

        // Validar que no se cruce con el descanso/almuerzo si existe
        if (horario.getHoraInicioDescanso() != null && horario.getHoraFinDescanso() != null) {
            boolean solapaDescanso = horaInicioCita.isBefore(horario.getHoraFinDescanso()) &&
                                     horaFinCita.isAfter(horario.getHoraInicioDescanso());
            if (solapaDescanso) {
                throw new IllegalArgumentException(String.format(
                        "El horario solicitado coincide con el periodo de descanso del profesional (%s - %s)",
                        horario.getHoraInicioDescanso(), horario.getHoraFinDescanso()
                ));
            }
        }
    }

    private String generarCodigoReserva() {
        return "RES-" + LocalDateTime.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public CitaDTO toDTO(Cita c) {
        return new CitaDTO(
                c.getId(),
                c.getCodigoReserva(),
                clienteService.toDTO(c.getCliente()),
                trabajadorService.toDTO(c.getTrabajador()),
                servicioService.toDTO(c.getServicio()),
                c.getFechaHoraInicio(),
                c.getFechaHoraFin(),
                c.getEstado(),
                c.getPrecioFinal(),
                c.getNotasCliente(),
                c.getNotasTrabajador(),
                c.getMotivoCancelacion()
        );
    }
}
