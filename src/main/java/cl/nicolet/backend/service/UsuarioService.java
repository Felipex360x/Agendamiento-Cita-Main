package cl.nicolet.backend.service;

import java.util.ArrayList;
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
import cl.nicolet.backend.model.Cliente;
import cl.nicolet.backend.model.HorarioDisponibilidad;
import cl.nicolet.backend.model.Servicio;
import cl.nicolet.backend.model.Trabajador;
import cl.nicolet.backend.repository.ClienteRepository;
import cl.nicolet.backend.repository.HorarioDisponibilidadRepository;
import cl.nicolet.backend.repository.ServicioRepository;
import cl.nicolet.backend.repository.TrabajadorRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;

@Service
@Transactional
public class UsuarioService {

    private static final Logger Log = LoggerFactory.getLogger(UsuarioService.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired(required = false)
    private TipoUsuarioRepository tipoUsuarioRepository;

    @Autowired(required = false)
    private ClienteRepository clienteRepository;

    @Autowired(required = false)
    private TrabajadorRepository trabajadorRepository;

    @Autowired(required = false)
    private ServicioRepository servicioRepository;

    @Autowired(required = false)
    private HorarioDisponibilidadRepository horarioDisponibilidadRepository;

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

        sincronizarPerfil(guardar);

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

        Usuario guardado = usuarioRepository.save(u);
        sincronizarPerfil(guardado);
        return toDTO(guardado);
    }

    private void sincronizarPerfil(Usuario u) {
        if (u.getTipoUsuario() == null) return;
        String rol = u.getTipoUsuario().getNombre();

        if ("CLIENTE".equalsIgnoreCase(rol) && clienteRepository != null) {
            if (!clienteRepository.existsByUsuarioId(u.getId())) {
                Log.info("Auto-creando perfil de Cliente para usuario id={}", u.getId());
                Cliente c = new Cliente();
                c.setUsuario(u);
                c.setActivo(true);
                clienteRepository.save(c);
            }
        } else if ("PROFESIONAL".equalsIgnoreCase(rol) && trabajadorRepository != null) {
            if (!trabajadorRepository.existsByUsuarioId(u.getId())) {
                Log.info("Auto-creando perfil de Trabajador para usuario id={}", u.getId());
                Trabajador t = new Trabajador();
                t.setUsuario(u);
                t.setCargoEspecialidad("Especialista General");
                t.setActivo(true);
                t.setComisionPorcentaje(BigDecimal.ZERO);

                if (servicioRepository != null) {
                    List<Servicio> servicios = servicioRepository.findByActivoTrue();
                    t.setServicios(new HashSet<>(servicios));
                }

                Trabajador tGuardado = trabajadorRepository.save(t);

                if (horarioDisponibilidadRepository != null) {
                    for (int dia = 1; dia <= 5; dia++) {
                        HorarioDisponibilidad h = new HorarioDisponibilidad();
                        h.setTrabajador(tGuardado);
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
