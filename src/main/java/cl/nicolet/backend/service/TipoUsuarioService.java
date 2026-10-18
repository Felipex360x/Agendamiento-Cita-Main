package cl.nicolet.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cl.nicolet.backend.dto.TipoUsuarioCreateDTO;
import cl.nicolet.backend.dto.TipoUsuarioDTO;
import cl.nicolet.backend.exception.RecursoNoEncontradoException;
import cl.nicolet.backend.model.TipoUsuario;
import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.repository.TipoUsuarioRepository;
import cl.nicolet.backend.repository.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TipoUsuarioService {

    private static final Logger Log = LoggerFactory.getLogger(TipoUsuarioService.class);

    @Autowired
    private TipoUsuarioRepository tipoUsuarioRepository;

    @Autowired(required = false)
    private UsuarioRepository usuarioRepository;

    public List<TipoUsuarioDTO> findAll() {
        Log.info("Consultando todos los tipos de usuario");
        return tipoUsuarioRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public TipoUsuarioDTO findById(Long id) {
        Log.info("Buscando TipoUsuario id={}", id);
        TipoUsuario tu = tipoUsuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de usuario no encontrado: " + id));
        return toDTO(tu);
    }

    public TipoUsuarioDTO crear(TipoUsuarioCreateDTO dto) {
        Log.info("Creando tipo de usuario nombre={}", dto.getNombre());
        if (tipoUsuarioRepository.existsByNombreIgnoreCase(dto.getNombre().trim())) {
            throw new IllegalArgumentException("Ya existe un tipo de usuario con el nombre: " + dto.getNombre());
        }
        TipoUsuario tu = new TipoUsuario();
        tu.setNombre(dto.getNombre().trim());
        tu.setDescripcion(dto.getDescripcion());
        TipoUsuario guardado = tipoUsuarioRepository.save(tu);
        Log.info("Tipo de usuario creado id={}", guardado.getId());
        return toDTO(guardado);
    }

    public TipoUsuarioDTO actualizar(Long id, TipoUsuarioCreateDTO dto) {
        Log.info("Actualizando tipo de usuario id={}", id);
        TipoUsuario tu = tipoUsuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de usuario no encontrado: " + id));

        tipoUsuarioRepository.findByNombreIgnoreCase(dto.getNombre().trim())
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new IllegalArgumentException("Ya existe otro tipo de usuario con el nombre: " + dto.getNombre());
                });

        tu.setNombre(dto.getNombre().trim());
        tu.setDescripcion(dto.getDescripcion());
        return toDTO(tipoUsuarioRepository.save(tu));
    }

    public void eliminar(Long id) {
        Log.info("Eliminando tipo de usuario id={}", id);
        if (!tipoUsuarioRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Tipo de usuario no encontrado: " + id);
        }
        if (usuarioRepository != null) {
            List<Usuario> usuariosConRol = usuarioRepository.findAll().stream()
                    .filter(u -> u.getTipoUsuario() != null && id.equals(u.getTipoUsuario().getId()))
                    .collect(Collectors.toList());
            for (Usuario u : usuariosConRol) {
                u.setTipoUsuario(null);
                usuarioRepository.save(u);
            }
        }
        tipoUsuarioRepository.deleteById(id);
        Log.info("Tipo de usuario id={} eliminado", id);
    }

    public TipoUsuarioDTO toDTO(TipoUsuario tu) {
        if (tu == null) {
            return null;
        }
        return new TipoUsuarioDTO(tu.getId(), tu.getNombre(), tu.getDescripcion());
    }

}
