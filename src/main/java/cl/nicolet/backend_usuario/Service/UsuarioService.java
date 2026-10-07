package cl.nicolet.backend_usuario.Service;

import java.util.ArrayList;
import java.util.List;

import java.util.stream.Collectors;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cl.nicolet.backend_usuario.Model.Usuario;

import  cl.nicolet.backend_usuario.Repository.UsuarioRepository;

import  cl.nicolet.backend_usuario.DTO.UsuarioCreateDTO;
import  cl.nicolet.backend_usuario.DTO.UsuarioDTO;

import  cl.nicolet.backend_usuario.Exception.RecursoNoEncontradoException;


@Service
public class UsuarioService {

    
    private static final Logger Log =LoggerFactory.getLogger(UsuarioService.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    public List<UsuarioDTO> findAll(){
        Log.info("Consultando a todos los usuarios");
        return usuarioRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public UsuarioDTO findById(Long id){
        Log.info("Buscando Usuario id={}",id);
        Usuario u = usuarioRepository.findById(id).orElseThrow(()-> new RecursoNoEncontradoException("Usuario no encontrado: "+id));
        Log.info("Usuario encontrado: nombre={}, correo={}", u.getNombre(), u.getCorreo());
        return toDTO(u);
    }
    public UsuarioDTO crear(UsuarioCreateDTO dto){
        Log.info("creando al usuario correo={}",dto.getCorreo());
        Usuario u = new Usuario();
        u.setNombre(dto.getNombre());
        u.setApellidoP(dto.getApellidoP());
        u.setCorreo(dto.getCorreo());
        u.setPassword(dto.getPassword());
        Usuario guardar = usuarioRepository.save(u);
        Log.info("usuario creado id={}",guardar.getId());
        return toDTO(guardar);
    }

    public UsuarioDTO actualizar(Long id,UsuarioCreateDTO dto){
        Log.info("actualizando usuario id={}",id);
        Usuario u = usuarioRepository.findById(id) .orElseThrow(()-> new RecursoNoEncontradoException("Usuario no encontrado: " +id));
        u.setNombre(dto.getNombre());
        u.setApellidoP(dto.getApellidoP());
        u.setCorreo(dto.getCorreo());
        u.setPassword(dto.getPassword());
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
        return new UsuarioDTO(
            u.getId(),
            u.getNombre(),
            u.getApellidoP(),
            u.getCorreo(),
            u.getPassword()
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
