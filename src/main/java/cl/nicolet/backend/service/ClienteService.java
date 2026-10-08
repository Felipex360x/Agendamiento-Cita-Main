package cl.nicolet.backend.service;

import cl.nicolet.backend.dto.ClienteCreateDTO;
import cl.nicolet.backend.dto.ClienteDTO;
import cl.nicolet.backend.dto.TipoUsuarioDTO;
import cl.nicolet.backend.dto.UsuarioDTO;
import cl.nicolet.backend.exception.RecursoNoEncontradoException;
import cl.nicolet.backend.model.Cliente;
import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.repository.ClienteRepository;
import cl.nicolet.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClienteService {

    private static final Logger log = LoggerFactory.getLogger(ClienteService.class);

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public List<ClienteDTO> findAll() {
        log.info("Consultando todos los clientes");
        return clienteRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public ClienteDTO findById(Long id) {
        log.info("Buscando cliente por id={}", id);
        Cliente c = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: " + id));
        return toDTO(c);
    }

    public ClienteDTO findByUsuarioId(Long usuarioId) {
        log.info("Buscando cliente por usuarioId={}", usuarioId);
        Cliente c = clienteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado para usuario: " + usuarioId));
        return toDTO(c);
    }

    public ClienteDTO crear(ClienteCreateDTO dto) {
        log.info("Creando perfil de cliente para usuarioId={}", dto.getUsuarioId());

        if (clienteRepository.existsByUsuarioId(dto.getUsuarioId())) {
            throw new IllegalArgumentException("El usuario ya tiene un perfil de cliente registrado");
        }

        if (dto.getRutDni() != null && !dto.getRutDni().isBlank() && clienteRepository.existsByRutDni(dto.getRutDni())) {
            throw new IllegalArgumentException("El RUT/DNI ya se encuentra registrado: " + dto.getRutDni());
        }

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + dto.getUsuarioId()));

        Cliente c = new Cliente();
        c.setUsuario(usuario);
        c.setTelefono(dto.getTelefono());
        c.setRutDni(dto.getRutDni());
        c.setFechaNacimiento(dto.getFechaNacimiento());
        c.setNotasPreferencias(dto.getNotasPreferencias());
        c.setActivo(true);

        return toDTO(clienteRepository.save(c));
    }

    public ClienteDTO actualizar(Long id, ClienteCreateDTO dto) {
        log.info("Actualizando cliente id={}", id);
        Cliente c = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: " + id));

        c.setTelefono(dto.getTelefono());
        c.setFechaNacimiento(dto.getFechaNacimiento());
        c.setNotasPreferencias(dto.getNotasPreferencias());

        return toDTO(clienteRepository.save(c));
    }

    public ClienteDTO toDTO(Cliente c) {
        Usuario u = c.getUsuario();
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

        return new ClienteDTO(
                c.getId(),
                userDTO,
                c.getTelefono(),
                c.getRutDni(),
                c.getFechaNacimiento(),
                c.getNotasPreferencias(),
                c.getActivo()
        );
    }
}
