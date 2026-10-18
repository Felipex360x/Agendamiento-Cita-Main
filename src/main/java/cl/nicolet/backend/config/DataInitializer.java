package cl.nicolet.backend.config;

import cl.nicolet.backend.model.*;
import cl.nicolet.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
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

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Verificando datos iniciales del sistema...");

        // 1. Roles / Tipos de usuario
        TipoUsuario tipoAdmin = obtenerOCrearTipo("ADMINISTRADOR", "Administrador con acceso total");
        TipoUsuario tipoCliente = obtenerOCrearTipo("CLIENTE", "Cliente que solicita y agenda citas");
        TipoUsuario tipoPro = obtenerOCrearTipo("PROFESIONAL", "Profesional o especialista que atiende las citas");

        // 2. Usuarios base y de prueba garantizados
        obtenerOCrearUsuario("Maximo", "Rojas", "maximo.rojas@estudio.cl", "password_seguro_123", tipoAdmin);
        obtenerOCrearUsuario("Admin", "Test", "admin.test@nicolet.cl", "AdminTest123!", tipoAdmin);
        obtenerOCrearUsuario("Administrador", "Nicolet", "admin@nicolet.cl", "Admin123!", tipoAdmin);
        obtenerOCrearUsuario("Nicolet", "Estudio", "contacto@nicollet.cl", "admin2026", tipoAdmin);
        Usuario userMartina = obtenerOCrearUsuario("Martina", "Contreras", "martina.contreras@gmail.com", "123412", tipoCliente);
        Usuario userCamila = obtenerOCrearUsuario("Camila", "Silva", "camila.silva@estudio.cl", "pro2026", tipoPro);
        Usuario userMarit = obtenerOCrearUsuario("Martina", "Soto", "marit@gmail.com", "123456", tipoCliente);
        Usuario userGale = obtenerOCrearUsuario("Felipe", "Galle", "gale@gmail.com", "123456", tipoCliente);
        obtenerOCrearUsuario("Prueba", "Admin", "prueba1235@gmail.com", "123456", tipoAdmin);

        // Migrar automáticamente a BCrypt cualquier usuario preexistente en la base de datos con contraseña en texto plano
        List<Usuario> todosLosUsuarios = usuarioRepository.findAll();
        for (Usuario u : todosLosUsuarios) {
            if (u.getPassword() != null && !u.getPassword().startsWith("$2a$") && !u.getPassword().startsWith("$2b$")) {
                log.info("Migrando contraseña en texto plano a BCrypt para usuario id={}, correo={}", u.getId(), u.getCorreo());
                u.setPassword(passwordEncoder.encode(u.getPassword()));
                usuarioRepository.save(u);
            }
        }

        // 3. Catálogo de Servicios
        Servicio sCorte = obtenerOCrearServicio("Corte de Cabello y Peinado", "Lavado, corte estilizado y secado profesional", 45, new BigDecimal("18000.00"));
        Servicio sManicura = obtenerOCrearServicio("Manicura Rusa con Esmaltado Permanente", "Limpieza profunda de cutículas y esmaltado de alta duración", 60, new BigDecimal("22000.00"));
        Servicio sFacial = obtenerOCrearServicio("Tratamiento Facial Hidratante", "Limpieza profunda, exfoliación y mascarilla regeneradora", 60, new BigDecimal("28000.00"));

        // 4. Perfiles de Cliente
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

        Cliente clienteMarit = clienteRepository.findByUsuarioId(userMarit.getId()).orElseGet(() -> {
            log.info("Inicializando perfil de Cliente para {}", userMarit.getNombre());
            Cliente c = new Cliente();
            c.setUsuario(userMarit);
            c.setTelefono("+56977778888");
            c.setRutDni("20123456-7");
            c.setFechaNacimiento(LocalDate.of(2000, 3, 20));
            c.setNotasPreferencias("Alérgica a productos con alcohol");
            c.setActivo(true);
            return clienteRepository.save(c);
        });

        clienteRepository.findByUsuarioId(userGale.getId()).orElseGet(() -> {
            log.info("Inicializando perfil de Cliente para {}", userGale.getNombre());
            Cliente c = new Cliente();
            c.setUsuario(userGale);
            c.setTelefono("+56966665555");
            c.setRutDni("18555444-3");
            c.setFechaNacimiento(LocalDate.of(1995, 11, 12));
            c.setNotasPreferencias("Cliente frecuente de corte de cabello");
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
            t.setServicios(new HashSet<>(List.of(sCorte, sManicura, sFacial)));
            return trabajadorRepository.save(t);
        });

        if (trabajadorCamila.getServicios() == null || trabajadorCamila.getServicios().size() < 3) {
            Set<Servicio> servs = trabajadorCamila.getServicios() != null ? new HashSet<>(trabajadorCamila.getServicios()) : new HashSet<>();
            servs.add(sCorte);
            servs.add(sManicura);
            servs.add(sFacial);
            trabajadorCamila.setServicios(servs);
            trabajadorCamila = trabajadorRepository.save(trabajadorCamila);
        }

        // 6. Horarios de atención para Camila (Lunes a Viernes 09:00 a 18:00, colación 13:00 - 14:00)
        List<HorarioDisponibilidad> horariosExistentes = horarioDisponibilidadRepository.findByTrabajadorId(trabajadorCamila.getId());
        Set<Integer> diasVistos = new HashSet<>();
        for (HorarioDisponibilidad hExistente : horariosExistentes) {
            if (Boolean.TRUE.equals(hExistente.getActivo()) && !diasVistos.add(hExistente.getDiaSemana())) {
                horarioDisponibilidadRepository.delete(hExistente);
            }
        }

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

        // 7. Citas semilla de ejemplo
        if (citaRepository.findByCodigoReserva("RES-2026-0001").isEmpty()) {
            log.info("Inicializando cita RES-2026-0001 (CONFIRMADA)...");
            Cita cita = new Cita();
            cita.setCodigoReserva("RES-2026-0001");
            cita.setCliente(clienteMartina);
            cita.setTrabajador(trabajadorCamila);
            cita.setServicio(sManicura);
            
            LocalDateTime proximoLunes = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
            cita.setFechaHoraInicio(proximoLunes);
            cita.setFechaHoraFin(proximoLunes.plusMinutes(sManicura.getDuracionMinutos()));
            cita.setEstado(EstadoCita.CONFIRMADA);
            cita.setPrecioFinal(sManicura.getPrecio());
            cita.setNotasCliente("Primera sesión de prueba");
            cita.setNotasTrabajador("Revisar cutículas previas");
            citaRepository.save(cita);
        }

        if (citaRepository.findByCodigoReserva("RES-2026-0002").isEmpty()) {
            log.info("Inicializando cita RES-2026-0002 (PENDIENTE)...");
            Cita cita = new Cita();
            cita.setCodigoReserva("RES-2026-0002");
            cita.setCliente(clienteMarit);
            cita.setTrabajador(trabajadorCamila);
            cita.setServicio(sCorte);
            
            LocalDateTime fechaPendiente = LocalDateTime.now().plusDays(2).withHour(11).withMinute(30).withSecond(0).withNano(0);
            cita.setFechaHoraInicio(fechaPendiente);
            cita.setFechaHoraFin(fechaPendiente.plusMinutes(sCorte.getDuracionMinutos()));
            cita.setEstado(EstadoCita.PENDIENTE);
            cita.setPrecioFinal(sCorte.getPrecio());
            cita.setNotasCliente("Cita agendada desde el portal web cliente - Requiere confirmación");
            cita.setNotasTrabajador(null);
            citaRepository.save(cita);
        }

        if (citaRepository.findByCodigoReserva("RES-2026-0003").isEmpty()) {
            log.info("Inicializando cita RES-2026-0003 (COMPLETADA)...");
            Cita cita = new Cita();
            cita.setCodigoReserva("RES-2026-0003");
            cita.setCliente(clienteMartina);
            cita.setTrabajador(trabajadorCamila);
            cita.setServicio(sFacial);
            
            LocalDateTime fechaCompletada = LocalDateTime.now().minusDays(4).withHour(15).withMinute(0).withSecond(0).withNano(0);
            cita.setFechaHoraInicio(fechaCompletada);
            cita.setFechaHoraFin(fechaCompletada.plusMinutes(sFacial.getDuracionMinutos()));
            cita.setEstado(EstadoCita.COMPLETADA);
            cita.setPrecioFinal(sFacial.getPrecio());
            cita.setNotasCliente("Tratamiento facial hidratante realizado");
            cita.setNotasTrabajador("Piel sensible, se aplicó emulsión calmante post tratamiento. Excelente evolución.");
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
        String encodedPass = (passwordEncoder != null && !pass.startsWith("$2a$") && !pass.startsWith("$2b$"))
                ? passwordEncoder.encode(pass)
                : pass;

        return usuarioRepository.findByCorreoIgnoreCase(correo)
                .map(u -> {
                    boolean modificado = false;
                    // Garantizar siempre la contraseña esperada para todos los usuarios semilla
                    if (passwordEncoder != null && !passwordEncoder.matches(pass, u.getPassword())) {
                        log.info("Sincronizando contraseña garantizada para usuario semilla {}", correo);
                        u.setPassword(encodedPass);
                        modificado = true;
                    }
                    if (u.getPassword() != null && !u.getPassword().startsWith("$2a$") && !u.getPassword().startsWith("$2b$")) {
                        log.info("Actualizando contraseña en texto plano a BCrypt para usuario {}", correo);
                        u.setPassword(encodedPass);
                        modificado = true;
                    }
                    if (u.getTipoUsuario() == null && tipo != null) {
                        u.setTipoUsuario(tipo);
                        modificado = true;
                    }
                    return modificado ? usuarioRepository.save(u) : u;
                })
                .orElseGet(() -> {
                    Usuario u = new Usuario();
                    u.setNombre(nombre);
                    u.setApellidoP(apellido);
                    u.setCorreo(correo);
                    u.setPassword(encodedPass);
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
