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

import java.math.BigDecimal;
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

    @Autowired
    private PerfilUsuarioService perfilUsuarioService;

    public List<TrabajadorDTO> findAll() {
        log.info("Consultando todos los trabajadores activos");
        return trabajadorRepository.findByActivoTrue().stream()
                .filter(trabajador -> trabajador.getUsuario() != null
                        && trabajador.getUsuario().getEstado() == Usuario.ESTADO_ACTIVO)
                .map(this::toPublicDTO)
                .collect(Collectors.toList());
    }

    private TrabajadorDTO toPublicDTO(Trabajador trabajador) {
        Usuario usuario = trabajador.getUsuario();
        TipoUsuarioDTO tipo = usuario.getTipoUsuario() == null ? null : new TipoUsuarioDTO(
                usuario.getTipoUsuario().getId(),
                usuario.getTipoUsuario().getNombre(),
                usuario.getTipoUsuario().getDescripcion()
        );
        UsuarioDTO usuarioPublico = new UsuarioDTO(
                usuario.getId(), usuario.getNombre(), usuario.getApellidoP(), null, tipo,
                Usuario.ESTADO_ACTIVO
        );
        Set<ServicioDTO> serviciosPublicos = trabajador.getServicios().stream()
                .map(servicio -> new ServicioDTO(
                        servicio.getId(), servicio.getNombre(), servicio.getDescripcion(),
                        servicio.getDuracionMinutos(), servicio.getPrecio(), servicio.getActivo()
                ))
                .collect(Collectors.toSet());

        // Public directory only needs identity, specialty, biography and offered services.
        return new TrabajadorDTO(
                trabajador.getId(), usuarioPublico, null, null,
                trabajador.getCargoEspecialidad(), trabajador.getBiografia(), null,
                trabajador.getActivo(), serviciosPublicos
        );
    }

    public boolean esPropietario(Long trabajadorId, Long usuarioId) {
        return trabajadorRepository.findByUsuarioId(usuarioId)
                .map(trabajador -> trabajador.getId().equals(trabajadorId))
                .orElse(false);
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

        String rutNormalizado = (dto.getRutDni() != null && !dto.getRutDni().isBlank()) ? dto.getRutDni().trim() : null;
        if (rutNormalizado != null && trabajadorRepository.existsByRutDni(rutNormalizado)) {
            throw new IllegalArgumentException("El RUT/DNI ya se encuentra registrado: " + rutNormalizado);
        }

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + dto.getUsuarioId()));

        Trabajador t = new Trabajador();
        t.setUsuario(usuario);
        t.setRutDni(rutNormalizado);
        t.setTelefono(dto.getTelefono());
        t.setCargoEspecialidad(dto.getCargoEspecialidad());
        t.setBiografia(dto.getBiografia());
        t.setComisionPorcentaje(dto.getComisionPorcentaje() != null ? dto.getComisionPorcentaje() : BigDecimal.ZERO);
        t.setActivo(true);

        if (dto.getServicioIds() != null && !dto.getServicioIds().isEmpty()) {
            Set<Servicio> servicios = new HashSet<>(servicioRepository.findAllById(dto.getServicioIds()));
            t.setServicios(servicios);
        } else if (servicioRepository != null) {
            t.setServicios(new HashSet<>(servicioRepository.findByActivoTrue()));
        }

        Trabajador guardado = trabajadorRepository.save(t);

        perfilUsuarioService.crearHorariosBaseSiFaltan(guardado);

        return toDTO(guardado);
    }

    public TrabajadorDTO actualizar(Long id, TrabajadorCreateDTO dto) {
        log.info("Actualizando trabajador id={}", id);
        Trabajador t = trabajadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Trabajador no encontrado: " + id));

        if (dto.getRutDni() != null && !dto.getRutDni().isBlank()) {
            String rutNormalizado = dto.getRutDni().trim();
            trabajadorRepository.findByRutDni(rutNormalizado)
                    .filter(existente -> !existente.getId().equals(id))
                    .ifPresent(existente -> {
                        throw new IllegalArgumentException("El RUT/DNI ya se encuentra registrado: " + rutNormalizado);
                    });
            t.setRutDni(rutNormalizado);
        }
        if (dto.getTelefono() != null) {
            t.setTelefono(dto.getTelefono());
        }
        if (dto.getCargoEspecialidad() != null && !dto.getCargoEspecialidad().isBlank()) {
            t.setCargoEspecialidad(dto.getCargoEspecialidad());
        }
        if (dto.getBiografia() != null) {
            t.setBiografia(dto.getBiografia());
        }
        if (dto.getComisionPorcentaje() != null) {
            t.setComisionPorcentaje(dto.getComisionPorcentaje());
        }

        if (dto.getServicioIds() != null && !dto.getServicioIds().isEmpty()) {
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
