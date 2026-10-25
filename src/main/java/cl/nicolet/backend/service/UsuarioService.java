package cl.nicolet.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cl.nicolet.backend.model.TipoUsuario;
import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.repository.TipoUsuarioRepository;
import cl.nicolet.backend.repository.UsuarioRepository;
import cl.nicolet.backend.dto.TipoUsuarioDTO;
import cl.nicolet.backend.dto.UsuarioCreateDTO;
import cl.nicolet.backend.dto.UsuarioDTO;
import cl.nicolet.backend.exception.RecursoNoEncontradoException;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UsuarioService {

    private static final Logger Log = LoggerFactory.getLogger(UsuarioService.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired(required = false)
    private TipoUsuarioRepository tipoUsuarioRepository;



    @Autowired(required = false)
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private PerfilUsuarioService perfilUsuarioService;

    public List<UsuarioDTO> findAll(){
        Log.info("Consultando a todos los usuarios");
        return usuarioRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public UsuarioDTO findById(Long id){
        Log.info("Buscando Usuario id={}", id);
        Usuario u = usuarioRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
        Log.info("Usuario encontrado: nombre={}, correo={}", u.getNombre(), u.getCorreo());
        return toDTO(u);
    }

    public UsuarioDTO crear(UsuarioCreateDTO dto){
        Log.info("creando al usuario correo={}", dto.getCorreo());
        String correo = dto.getCorreo() != null ? dto.getCorreo().trim().toLowerCase() : "";
        if (!correo.isBlank() && usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new IllegalArgumentException("El correo electrónico ya se encuentra registrado: " + correo);
        }

        Usuario u = new Usuario();
        u.setNombre(dto.getNombre());
        u.setApellidoP(dto.getApellidoP());
        u.setCorreo(correo);

        String pass = dto.getPassword();
        if (pass == null || pass.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria para registrar un nuevo usuario");
        }
        if (pass.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("la contraseña no puede exceder los 72 caracteres");
        }
        if (passwordEncoder != null && !pass.startsWith("$2a$") && !pass.startsWith("$2b$")) {
            pass = passwordEncoder.encode(pass);
        }
        u.setPassword(pass);

        if (dto.getTipoUsuarioId() != null && tipoUsuarioRepository != null) {
            TipoUsuario tipo = tipoUsuarioRepository.findById(dto.getTipoUsuarioId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de usuario no encontrado: " + dto.getTipoUsuarioId()));
            u.setTipoUsuario(tipo);
        }

        Usuario guardar = usuarioRepository.save(u);
        Log.info("usuario creado id={}", guardar.getId());

        if (guardar.getTipoUsuario() != null) {
            perfilUsuarioService.sincronizarPerfil(guardar);
        }

        return toDTO(guardar);
    }

    public UsuarioDTO actualizar(Long id, UsuarioCreateDTO dto){
        Log.info("actualizando usuario id={}", id);
        Usuario u = usuarioRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));

        String nuevoCorreo = dto.getCorreo() != null ? dto.getCorreo().trim().toLowerCase() : "";
        if (!nuevoCorreo.isBlank() && !nuevoCorreo.equalsIgnoreCase(u.getCorreo()) && usuarioRepository.existsByCorreoIgnoreCase(nuevoCorreo)) {
            throw new IllegalArgumentException("El correo electrónico ya se encuentra registrado: " + nuevoCorreo);
        }

        u.setNombre(dto.getNombre());
        u.setApellidoP(dto.getApellidoP());
        u.setCorreo(nuevoCorreo);

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            String pass = dto.getPassword();
            if (pass.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                throw new IllegalArgumentException("la contraseña no puede exceder los 72 caracteres");
            }
            if (passwordEncoder != null && !pass.startsWith("$2a$") && !pass.startsWith("$2b$")) {
                pass = passwordEncoder.encode(pass);
            }
            u.setPassword(pass);
        }

        if (dto.getTipoUsuarioId() != null && tipoUsuarioRepository != null) {
            TipoUsuario tipo = tipoUsuarioRepository.findById(dto.getTipoUsuarioId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de usuario no encontrado: " + dto.getTipoUsuarioId()));
            u.setTipoUsuario(tipo);
        }

        Usuario guardado = usuarioRepository.save(u);
        if (guardado.getTipoUsuario() != null) {
            perfilUsuarioService.sincronizarPerfil(guardado);
        }
        return toDTO(guardado);
    }

    public UsuarioDTO desactivar(Long id, String motivo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
        String motivoLimpio = motivo == null ? "" : motivo.trim();
        if (motivoLimpio.isBlank()) {
            throw new IllegalArgumentException("Debes indicar el motivo de desactivación");
        }
        usuario.setEstado(Usuario.ESTADO_DESACTIVADO);
        usuario.setMotivoDesactivacion(motivoLimpio);
        Usuario guardado = usuarioRepository.save(usuario);
        Log.info("Usuario id={} desactivado; se conservaron perfiles y registros asociados", id);
        return toDTO(guardado);
    }

    public UsuarioDTO activar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
        usuario.setEstado(Usuario.ESTADO_ACTIVO);
        usuario.setMotivoDesactivacion(null);
        Usuario guardado = usuarioRepository.save(usuario);
        Log.info("Usuario id={} reactivado", id);
        return toDTO(guardado);
    }

    private UsuarioDTO toDTO(Usuario u) {
        TipoUsuarioDTO tipoDTO = null;
        if (u.getTipoUsuario() != null) {
            tipoDTO = new TipoUsuarioDTO(
                u.getTipoUsuario().getId(),
                u.getTipoUsuario().getNombre(),
                u.getTipoUsuario().getDescripcion()
            );
        }
        return new UsuarioDTO(
            u.getId(),
            u.getNombre(),
            u.getApellidoP(),
            u.getCorreo(),
            tipoDTO,
            u.getEstado(),
            u.getMotivoDesactivacion()
        );
    }

}
