package cl.nicolet.backend.service;

import cl.nicolet.backend.model.Cliente;
import cl.nicolet.backend.model.HorarioDisponibilidad;
import cl.nicolet.backend.model.Servicio;
import cl.nicolet.backend.model.Trabajador;
import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.repository.ClienteRepository;
import cl.nicolet.backend.repository.HorarioDisponibilidadRepository;
import cl.nicolet.backend.repository.ServicioRepository;
import cl.nicolet.backend.repository.TrabajadorRepository;
import cl.nicolet.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Locale;

@Service
@Transactional
public class PerfilUsuarioService {

    private static final LocalTime HORA_INICIO = LocalTime.of(9, 0);
    private static final LocalTime HORA_FIN = LocalTime.of(18, 0);
    private static final LocalTime INICIO_DESCANSO = LocalTime.of(13, 0);
    private static final LocalTime FIN_DESCANSO = LocalTime.of(14, 0);

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final ServicioRepository servicioRepository;
    private final HorarioDisponibilidadRepository horarioDisponibilidadRepository;

    public PerfilUsuarioService(
            UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository,
            TrabajadorRepository trabajadorRepository,
            ServicioRepository servicioRepository,
            HorarioDisponibilidadRepository horarioDisponibilidadRepository) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.trabajadorRepository = trabajadorRepository;
        this.servicioRepository = servicioRepository;
        this.horarioDisponibilidadRepository = horarioDisponibilidadRepository;
    }

    /** Actualiza o crea el perfil de cliente o trabajador cuando cambia el rol del usuario. */
    public void sincronizarPerfil(Usuario usuario) {
        if (usuario.getTipoUsuario() == null || usuario.getTipoUsuario().getNombre() == null) {
            // Si queda sin rol, se desactivan sus perfiles pero sin borrar sus datos ni historial.
            desactivarCliente(usuario);
            desactivarTrabajador(usuario);
            return;
        }

        String rol = usuario.getTipoUsuario().getNombre().toUpperCase(Locale.ROOT);
        if (rol.contains("CLIENTE")) {
            Cliente cliente = obtenerOCrearPerfilCliente(usuario, null);
            if (!Boolean.TRUE.equals(cliente.getActivo())) {
                cliente.setActivo(true);
                clienteRepository.save(cliente);
            }
            desactivarTrabajador(usuario);
        } else if (esRolProfesional(rol)) {
            Trabajador trabajador = obtenerOCrearPerfilTrabajador(usuario);
            if (!Boolean.TRUE.equals(trabajador.getActivo())) {
                trabajador.setActivo(true);
                trabajadorRepository.save(trabajador);
            }
        } else {
            desactivarTrabajador(usuario);
        }
    }

    /** Crea el perfil de cliente durante el registro guardando su teléfono. */
    public Cliente crearPerfilCliente(Usuario usuario, String telefono) {
        return obtenerOCrearPerfilCliente(usuario, telefono);
    }

    /** Crea los perfiles que falten para los usuarios existentes al iniciar el sistema. */
    public void crearPerfilesFaltantes() {
        for (Usuario usuario : usuarioRepository.findAll()) {
            if (usuario.getTipoUsuario() == null || usuario.getTipoUsuario().getNombre() == null) {
                continue;
            }

            String rol = usuario.getTipoUsuario().getNombre().toUpperCase(Locale.ROOT);
            if (rol.contains("CLIENTE")) {
                if (clienteRepository.findByUsuarioId(usuario.getId()).isEmpty()) {
                    obtenerOCrearPerfilCliente(usuario, null);
                }
            } else if (esRolProfesional(rol)
                    && trabajadorRepository.findByUsuarioId(usuario.getId()).isEmpty()) {
                obtenerOCrearPerfilTrabajador(usuario);
            }
        }
    }

    /** Agrega el horario normal de lunes a viernes si el nuevo trabajador aún no tiene horario. */
    public void crearHorariosBaseSiFaltan(Trabajador trabajador) {
        if (!horarioDisponibilidadRepository.findByTrabajadorId(trabajador.getId()).isEmpty()) {
            return;
        }

        for (int dia = 1; dia <= 5; dia++) {
            HorarioDisponibilidad horario = new HorarioDisponibilidad();
            horario.setTrabajador(trabajador);
            horario.setDiaSemana(dia);
            horario.setHoraInicio(HORA_INICIO);
            horario.setHoraFin(HORA_FIN);
            horario.setHoraInicioDescanso(INICIO_DESCANSO);
            horario.setHoraFinDescanso(FIN_DESCANSO);
            horario.setActivo(true);
            horarioDisponibilidadRepository.save(horario);
        }
    }

    private Cliente obtenerOCrearPerfilCliente(Usuario usuario, String telefono) {
        return clienteRepository.findByUsuarioId(usuario.getId()).orElseGet(() -> {
            Cliente cliente = new Cliente();
            cliente.setUsuario(usuario);
            cliente.setTelefono(telefono);
            cliente.setActivo(true);
            return clienteRepository.save(cliente);
        });
    }

    private Trabajador obtenerOCrearPerfilTrabajador(Usuario usuario) {
        return trabajadorRepository.findByUsuarioId(usuario.getId()).orElseGet(() -> {
            Trabajador trabajador = new Trabajador();
            trabajador.setUsuario(usuario);
            trabajador.setCargoEspecialidad("Especialista General");
            trabajador.setActivo(true);
            trabajador.setComisionPorcentaje(BigDecimal.ZERO);
            trabajador.setServicios(new HashSet<>(servicioRepository.findByActivoTrue()));

            Trabajador guardado = trabajadorRepository.save(trabajador);
            crearHorariosBaseSiFaltan(guardado);
            return guardado;
        });
    }

    private void desactivarCliente(Usuario usuario) {
        clienteRepository.findByUsuarioId(usuario.getId()).ifPresent(cliente -> {
            if (Boolean.TRUE.equals(cliente.getActivo())) {
                cliente.setActivo(false);
                clienteRepository.save(cliente);
            }
        });
    }

    private void desactivarTrabajador(Usuario usuario) {
        trabajadorRepository.findByUsuarioId(usuario.getId()).ifPresent(trabajador -> {
            if (Boolean.TRUE.equals(trabajador.getActivo())) {
                trabajador.setActivo(false);
                trabajadorRepository.save(trabajador);
            }
        });
    }

    private boolean esRolProfesional(String rol) {
        return rol.contains("PROFESIONAL") || rol.contains("TRABAJADOR") || rol.contains("ESPECIALISTA");
    }
}
