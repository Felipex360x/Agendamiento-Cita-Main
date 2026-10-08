package cl.nicolet.backend.config;

import cl.nicolet.backend.model.*;
import cl.nicolet.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private TipoUsuarioRepository tipoUsuarioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TrabajadorRepository trabajadorRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private HorarioDisponibilidadRepository horarioDisponibilidadRepository;

    @Autowired
    private CitaRepository citaRepository;

    @Override
    public void run(String... args) {
        log.info("Verificando datos iniciales del sistema...");

        // 1. Roles / Tipos de usuario
        TipoUsuario tipoAdmin = obtenerOCrearTipo("ADMINISTRADOR", "Administrador con acceso total");
        TipoUsuario tipoCliente = obtenerOCrearTipo("CLIENTE", "Cliente que solicita y agenda citas");
        TipoUsuario tipoPro = obtenerOCrearTipo("PROFESIONAL", "Profesional o especialista que atiende las citas");

        // 2. Usuarios base
        Usuario admin = obtenerOCrearUsuario("Maximo", "Rojas", "maximo.rojas@estudio.cl", "password_seguro_123", tipoAdmin);
        Usuario userMartina = obtenerOCrearUsuario("Martina", "Contreras", "martina.contreras@gmail.com", "123412", tipoCliente);
        Usuario userCamila = obtenerOCrearUsuario("Camila", "Silva", "camila.silva@estudio.cl", "pro2026", tipoPro);

        // 3. Catálogo de Servicios
        Servicio sCorte = obtenerOCrearServicio("Corte de Cabello y Peinado", "Lavado, corte estilizado y secado profesional", 45, new BigDecimal("18000.00"));
        Servicio sManicura = obtenerOCrearServicio("Manicura Rusa con Esmaltado Permanente", "Limpieza profunda de cutículas y esmaltado de alta duración", 60, new BigDecimal("22000.00"));
        Servicio sFacial = obtenerOCrearServicio("Tratamiento Facial Hidratante", "Limpieza profunda, exfoliación y mascarilla regeneradora", 60, new BigDecimal("28000.00"));

        // 4. Perfil Cliente para Martina
        Cliente clienteMartina = clienteRepository.findByUsuarioId(userMartina.getId()).orElseGet(() -> {
            log.info("Inicializando perfil de Cliente para {}", userMartina.getNombre());
            Cliente c = new Cliente();
            c.setUsuario(userMartina);
            c.setTelefono("+56912345678");
            c.setRutDni("19876543-2");
            c.setFechaNacimiento(LocalDate.of(1998, 5, 14));
            c.setNotasPreferencias("Prefiere tonos neutros para manicura");
            c.setActivo(true);
            return clienteRepository.save(c);
        });

        // 5. Perfil Trabajador para Camila
        Trabajador trabajadorCamila = trabajadorRepository.findByUsuarioId(userCamila.getId()).orElseGet(() -> {
            log.info("Inicializando perfil de Trabajador para {}", userCamila.getNombre());
            Trabajador t = new Trabajador();
            t.setUsuario(userCamila);
            t.setRutDni("18234567-8");
            t.setTelefono("+56987654321");
            t.setCargoEspecialidad("Especialista en Estilismo y Manicura");
            t.setBiografia("5 años de experiencia en estética integral");
            t.setComisionPorcentaje(new BigDecimal("30.00"));
            t.setActivo(true);
            t.setServicios(new HashSet<>(List.of(sCorte, sManicura)));
            return trabajadorRepository.save(t);
        });

        // 6. Horarios de atención para Camila (Lunes a Viernes 09:00 a 18:00, colación 13:00 - 14:00)
        for (int dia = 1; dia <= 5; dia++) {
            final int diaActual = dia;
            if (horarioDisponibilidadRepository.findByTrabajadorIdAndDiaSemanaAndActivoTrue(trabajadorCamila.getId(), diaActual).isEmpty()) {
                HorarioDisponibilidad h = new HorarioDisponibilidad();
                h.setTrabajador(trabajadorCamila);
                h.setDiaSemana(diaActual);
                h.setHoraInicio(LocalTime.of(9, 0));
                h.setHoraFin(LocalTime.of(18, 0));
                h.setHoraInicioDescanso(LocalTime.of(13, 0));
                h.setHoraFinDescanso(LocalTime.of(14, 0));
                h.setActivo(true);
                horarioDisponibilidadRepository.save(h);
            }
        }

        // 7. Cita semilla de ejemplo
        if (citaRepository.count() == 0) {
            log.info("Inicializando cita de prueba...");
            Cita cita = new Cita();
            cita.setCodigoReserva("RES-2026-0001");
            cita.setCliente(clienteMartina);
            cita.setTrabajador(trabajadorCamila);
            cita.setServicio(sManicura);
            
            // Programar para el próximo día hábil a las 10:00
            LocalDateTime proximoLunes = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
            cita.setFechaHoraInicio(proximoLunes);
            cita.setFechaHoraFin(proximoLunes.plusMinutes(sManicura.getDuracionMinutos()));
            cita.setEstado(EstadoCita.CONFIRMADA);
            cita.setPrecioFinal(sManicura.getPrecio());
            cita.setNotasCliente("Primera sesión de prueba");
            cita.setNotasTrabajador("Revisar cutículas previas");
            citaRepository.save(cita);
        }

        log.info("Datos iniciales verificados correctamente.");
    }

    private TipoUsuario obtenerOCrearTipo(String nombre, String descripcion) {
        return tipoUsuarioRepository.findByNombreIgnoreCase(nombre).orElseGet(() -> {
            TipoUsuario t = new TipoUsuario();
            t.setNombre(nombre);
            t.setDescripcion(descripcion);
            return tipoUsuarioRepository.save(t);
        });
    }

    private Usuario obtenerOCrearUsuario(String nombre, String apellido, String correo, String pass, TipoUsuario tipo) {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getCorreo().equalsIgnoreCase(correo))
                .findFirst()
                .orElseGet(() -> {
                    Usuario u = new Usuario();
                    u.setNombre(nombre);
                    u.setApellidoP(apellido);
                    u.setCorreo(correo);
                    u.setPassword(pass);
                    u.setTipoUsuario(tipo);
                    return usuarioRepository.save(u);
                });
    }

    private Servicio obtenerOCrearServicio(String nombre, String descripcion, int duracion, BigDecimal precio) {
        return servicioRepository.findByNombreIgnoreCase(nombre).orElseGet(() -> {
            Servicio s = new Servicio();
            s.setNombre(nombre);
            s.setDescripcion(descripcion);
            s.setDuracionMinutos(duracion);
            s.setPrecio(precio);
            s.setActivo(true);
            return servicioRepository.save(s);
        });
    }
}
