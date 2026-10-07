package cl.nicolet.backend.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cl.nicolet.backend.Model.TipoUsuario;
import cl.nicolet.backend.Model.Usuario;
import cl.nicolet.backend.Repository.TipoUsuarioRepository;
import cl.nicolet.backend.Repository.UsuarioRepository;
import cl.nicolet.backend.DTO.TipoUsuarioDTO;
import cl.nicolet.backend.DTO.UsuarioCreateDTO;
import cl.nicolet.backend.DTO.UsuarioDTO;
import cl.nicolet.backend.Exception.RecursoNoEncontradoException;

@Service
public class UsuarioService {

    private static final Logger Log = LoggerFactory.getLogger(UsuarioService.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired(required = false)
    private TipoUsuarioRepository tipoUsuarioRepository;

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
        Usuario u = new Usuario();
        u.setNombre(dto.getNombre());
        u.setApellidoP(dto.getApellidoP());
        u.setCorreo(dto.getCorreo());
        u.setPassword(dto.getPassword());

        if (dto.getTipoUsuarioId() != null && tipoUsuarioRepository != null) {
            TipoUsuario tipo = tipoUsuarioRepository.findById(dto.getTipoUsuarioId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de usuario no encontrado: " + dto.getTipoUsuarioId()));
            u.setTipoUsuario(tipo);
        }

        Usuario guardar = usuarioRepository.save(u);
        Log.info("usuario creado id={}", guardar.getId());
        return toDTO(guardar);
    }

    public UsuarioDTO actualizar(Long id, UsuarioCreateDTO dto){
        Log.info("actualizando usuario id={}", id);
        Usuario u = usuarioRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
        u.setNombre(dto.getNombre());
        u.setApellidoP(dto.getApellidoP());
        u.setCorreo(dto.getCorreo());
        u.setPassword(dto.getPassword());

        if (dto.getTipoUsuarioId() != null && tipoUsuarioRepository != null) {
            TipoUsuario tipo = tipoUsuarioRepository.findById(dto.getTipoUsuarioId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de usuario no encontrado: " + dto.getTipoUsuarioId()));
            u.setTipoUsuario(tipo);
        }

        return toDTO(usuarioRepository.save(u));
    }

    public void eliminar(Long id) {
        Log.info("Eliminando Usuario id={}", id);
        if (!usuarioRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Usuario no encontrado: " + id);
        }
        usuarioRepository.deleteById(id);
        Log.info("Usuario id={} eliminado", id);
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
            u.getPassword(),
            tipoDTO
        );
    }

    public List<String> validarUsuarioManual(UsuarioCreateDTO dto){
        List<String> errores = new ArrayList<>();
        if(usuarioRepository.existsByCorreoIgnoreCase(dto.getCorreo())){
            errores.add("el correo ya esta registrado");
        }
        return errores;
    }

}
