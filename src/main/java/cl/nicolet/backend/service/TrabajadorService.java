package cl.nicolet.backend.service;

import cl.nicolet.backend.dto.ServicioDTO;
import cl.nicolet.backend.dto.TipoUsuarioDTO;
import cl.nicolet.backend.dto.TrabajadorCreateDTO;
import cl.nicolet.backend.dto.TrabajadorDTO;
import cl.nicolet.backend.dto.UsuarioDTO;
import cl.nicolet.backend.exception.RecursoNoEncontradoException;
import cl.nicolet.backend.model.Servicio;
import cl.nicolet.backend.model.Trabajador;
import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.repository.ServicioRepository;
import cl.nicolet.backend.repository.TrabajadorRepository;
import cl.nicolet.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cl.nicolet.backend.model.HorarioDisponibilidad;
import cl.nicolet.backend.repository.HorarioDisponibilidadRepository;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TrabajadorService {

    private static final Logger log = LoggerFactory.getLogger(TrabajadorService.class);

    @Autowired
    private TrabajadorRepository trabajadorRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired(required = false)
    private HorarioDisponibilidadRepository horarioDisponibilidadRepository;

    public List<TrabajadorDTO> findAll() {
        log.info("Consultando todos los trabajadores activos");
        sincronizarUsuariosTrabajadores();
        return trabajadorRepository.findByActivoTrue().stream().map(this::toDTO).collect(Collectors.toList());
    }

    private void sincronizarUsuariosTrabajadores() {
        if (usuarioRepository == null || trabajadorRepository == null) return;
        List<Usuario> usuarios = usuarioRepository.findAll();
        for (Usuario u : usuarios) {
            if (u.getTipoUsuario() != null && "PROFESIONAL".equalsIgnoreCase(u.getTipoUsuario().getNombre())) {
                if (!trabajadorRepository.existsByUsuarioId(u.getId())) {
                    Trabajador t = new Trabajador();
                    t.setUsuario(u);
                    t.setCargoEspecialidad("Especialista General");
                    t.setActivo(true);
                    t.setComisionPorcentaje(BigDecimal.ZERO);
                    if (servicioRepository != null) {
                        t.setServicios(new HashSet<>(servicioRepository.findByActivoTrue()));
                    }
                    Trabajador guardado = trabajadorRepository.save(t);

                    if (horarioDisponibilidadRepository != null) {
                        for (int dia = 1; dia <= 5; dia++) {
                            HorarioDisponibilidad h = new HorarioDisponibilidad();
                            h.setTrabajador(guardado);
                            h.setDiaSemana(dia);
                            h.setHoraInicio(LocalTime.of(9, 0));
                            h.setHoraFin(LocalTime.of(18, 0));
                            h.setHoraInicioDescanso(LocalTime.of(13, 0));
                            h.setHoraFinDescanso(LocalTime.of(14, 0));
                            h.setActivo(true);
                            horarioDisponibilidadRepository.save(h);
                        }
                    }
                }
            }
        }
    }

    public TrabajadorDTO findById(Long id) {
        log.info("Buscando trabajador por id={}", id);
        Trabajador t = trabajadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Trabajador no encontrado: " + id));
        return toDTO(t);
    }

    public TrabajadorDTO findByUsuarioId(Long usuarioId) {
        log.info("Buscando trabajador por usuarioId={}", usuarioId);
        Trabajador t = trabajadorRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Trabajador no encontrado para usuario: " + usuarioId));
        return toDTO(t);
    }

    public TrabajadorDTO crear(TrabajadorCreateDTO dto) {
        log.info("Creando perfil de trabajador para usuarioId={}", dto.getUsuarioId());

        if (trabajadorRepository.existsByUsuarioId(dto.getUsuarioId())) {
            throw new IllegalArgumentException("El usuario ya tiene un perfil de trabajador registrado");
        }

        if (dto.getRutDni() != null && !dto.getRutDni().isBlank() && trabajadorRepository.existsByRutDni(dto.getRutDni())) {
            throw new IllegalArgumentException("El RUT/DNI ya se encuentra registrado: " + dto.getRutDni());
        }

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + dto.getUsuarioId()));

        Trabajador t = new Trabajador();
        t.setUsuario(usuario);
        t.setRutDni(dto.getRutDni());
        t.setTelefono(dto.getTelefono());
        t.setCargoEspecialidad(dto.getCargoEspecialidad());
        t.setBiografia(dto.getBiografia());
        t.setComisionPorcentaje(dto.getComisionPorcentaje() != null ? dto.getComisionPorcentaje() : BigDecimal.ZERO);
        t.setActivo(true);

        if (dto.getServicioIds() != null && !dto.getServicioIds().isEmpty()) {
            Set<Servicio> servicios = new HashSet<>(servicioRepository.findAllById(dto.getServicioIds()));
            t.setServicios(servicios);
        }

        return toDTO(trabajadorRepository.save(t));
    }

    public TrabajadorDTO actualizar(Long id, TrabajadorCreateDTO dto) {
        log.info("Actualizando trabajador id={}", id);
        Trabajador t = trabajadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Trabajador no encontrado: " + id));

        t.setTelefono(dto.getTelefono());
        t.setCargoEspecialidad(dto.getCargoEspecialidad());
        t.setBiografia(dto.getBiografia());
        if (dto.getComisionPorcentaje() != null) {
            t.setComisionPorcentaje(dto.getComisionPorcentaje());
        }

        if (dto.getServicioIds() != null) {
            Set<Servicio> servicios = new HashSet<>(servicioRepository.findAllById(dto.getServicioIds()));
            t.setServicios(servicios);
        }

        return toDTO(trabajadorRepository.save(t));
    }

    public TrabajadorDTO toDTO(Trabajador t) {
        Usuario u = t.getUsuario();
        TipoUsuarioDTO tipoDTO = null;
        if (u.getTipoUsuario() != null) {
            tipoDTO = new TipoUsuarioDTO(
                    u.getTipoUsuario().getId(),
                    u.getTipoUsuario().getNombre(),
                    u.getTipoUsuario().getDescripcion()
            );
        }
        UsuarioDTO userDTO = new UsuarioDTO(
                u.getId(),
                u.getNombre(),
                u.getApellidoP(),
                u.getCorreo(),
                u.getPassword(),
                tipoDTO
        );

        Set<ServicioDTO> servicioDTOs = t.getServicios().stream()
                .map(s -> new ServicioDTO(
                        s.getId(),
                        s.getNombre(),
                        s.getDescripcion(),
                        s.getDuracionMinutos(),
                        s.getPrecio(),
                        s.getActivo()
                ))
                .collect(Collectors.toSet());

        return new TrabajadorDTO(
                t.getId(),
                userDTO,
                t.getRutDni(),
                t.getTelefono(),
                t.getCargoEspecialidad(),
                t.getBiografia(),
                t.getComisionPorcentaje(),
                t.getActivo(),
                servicioDTOs
        );
    }
}
