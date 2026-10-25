package cl.nicolet.backend.config;

import cl.nicolet.backend.model.*;
import cl.nicolet.backend.repository.*;
import cl.nicolet.backend.service.PerfilUsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true")
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

    @Autowired
    private PerfilUsuarioService perfilUsuarioService;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Verificando datos iniciales del sistema...");

        TipoUsuario tipoAdmin = obtenerOCrearTipo("ADMINISTRADOR", "Administrador con acceso total");
        TipoUsuario tipoCliente = obtenerOCrearTipo("CLIENTE", "Cliente que solicita y agenda citas");
        TipoUsuario tipoPro = obtenerOCrearTipo("PROFESIONAL", "Profesional o especialista que atiende las citas");

        obtenerOCrearUsuario("Maximo", "Rojas", "maximo.rojas@estudio.cl", "password_seguro_123", tipoAdmin);
        obtenerOCrearUsuario("Admin", "Test", "admin.test@nicolet.cl", "AdminTest123!", tipoAdmin);
        obtenerOCrearUsuario("Administrador", "Nicolet", "admin@nicolet.cl", "Admin123!", tipoAdmin);
        obtenerOCrearUsuario("Nicolet", "Estudio", "contacto@nicollet.cl", "admin2026", tipoAdmin);
        Usuario userMartina = obtenerOCrearUsuario("Martina", "Contreras", "martina.contreras@gmail.com", "123412", tipoCliente);
        Usuario userCamila = obtenerOCrearUsuario("Camila", "Silva", "camila.silva@estudio.cl", "pro2026", tipoPro);
        Usuario userMarit = obtenerOCrearUsuario("Martina", "Soto", "marit@gmail.com", "123456", tipoCliente);
        Usuario userGale = obtenerOCrearUsuario("Felipe", "Galle", "gale@gmail.com", "123456", tipoCliente);
        obtenerOCrearUsuario("Prueba", "Admin", "prueba1235@gmail.com", "123456", tipoAdmin);

        // Cuentas adicionales de demostración, creadas una sola vez por correo.
        Usuario userValentina = obtenerOCrearUsuario("Valentina", "Pérez", "valentina.perez@demo.nicolet.cl", "Demo123!", tipoCliente);
        Usuario userAntonia = obtenerOCrearUsuario("Antonia", "Muñoz", "antonia.munoz@demo.nicolet.cl", "Demo123!", tipoCliente);
        Usuario userConstanza = obtenerOCrearUsuario("Constanza", "Díaz", "constanza.diaz@demo.nicolet.cl", "Demo123!", tipoCliente);
        Usuario userFernanda = obtenerOCrearUsuario("Fernanda", "Castro", "fernanda.castro@demo.nicolet.cl", "Demo123!", tipoCliente);
        Usuario userSofia = obtenerOCrearUsuario("Sofía", "Herrera", "sofia.herrera@demo.nicolet.cl", "Demo123!", tipoCliente);
        Usuario userFrancisca = obtenerOCrearUsuario("Francisca", "Vidal", "francisca.vidal@demo.nicolet.cl", "Demo123!", tipoPro);
        Usuario userValeria = obtenerOCrearUsuario("Valeria", "Araya", "valeria.araya@demo.nicolet.cl", "Demo123!", tipoPro);
        Usuario userDaniela = obtenerOCrearUsuario("Daniela", "Muñoz", "daniela.munoz@demo.nicolet.cl", "Demo123!", tipoPro);

        // Encriptar contraseñas en texto plano si existen
        List<Usuario> todosLosUsuarios = usuarioRepository.findAll();
        for (Usuario u : todosLosUsuarios) {
            if (u.getPassword() != null && !u.getPassword().startsWith("$2a$") && !u.getPassword().startsWith("$2b$")) {
                log.info("Migrando contraseña en texto plano a BCrypt para usuario id={}, correo={}", u.getId(), u.getCorreo());
                u.setPassword(passwordEncoder.encode(u.getPassword()));
                usuarioRepository.save(u);
            }
        }

        Servicio sCorte = obtenerOCrearServicio("Corte de Cabello y Peinado", "Lavado, corte estilizado y secado profesional", 45, new BigDecimal("18000.00"));
        Servicio sManicura = obtenerOCrearServicio("Manicura Rusa con Esmaltado Permanente", "Limpieza profunda de cutículas y esmaltado de alta duración", 60, new BigDecimal("22000.00"));
        Servicio sFacial = obtenerOCrearServicio("Tratamiento Facial Hidratante", "Limpieza profunda, exfoliación y mascarilla regeneradora", 60, new BigDecimal("28000.00"));
        Servicio sBalayage = obtenerOCrearServicio("Coloración y Balayage", "Diagnóstico de color, balayage y tratamiento protector", 180, new BigDecimal("65000.00"));
        Servicio sPedicure = obtenerOCrearServicio("Pedicure Spa", "Cuidado de uñas, exfoliación e hidratación de pies", 75, new BigDecimal("28000.00"));
        Servicio sCejas = obtenerOCrearServicio("Perfilado y Diseño de Cejas", "Diseño personalizado y perfilado con pinza", 30, new BigDecimal("12000.00"));
        Servicio sPestanas = obtenerOCrearServicio("Lifting de Pestañas", "Lifting, tinte y nutrición de pestañas", 60, new BigDecimal("25000.00"));
        Servicio sMasaje = obtenerOCrearServicio("Masaje Relajante", "Masaje corporal relajante de cuerpo completo", 60, new BigDecimal("32000.00"));

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

        Cliente clienteGale = clienteRepository.findByUsuarioId(userGale.getId()).orElseGet(() -> {
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

        Cliente clienteValentina = obtenerOCrearClienteDemo(userValentina, "+56970001001", "17111111-1", LocalDate.of(1997, 2, 8), "Prefiere horarios durante la mañana");
        Cliente clienteAntonia = obtenerOCrearClienteDemo(userAntonia, " +56970001002", "17111112-0", LocalDate.of(2001, 7, 19), "Prefiere esmaltes de larga duración");
        Cliente clienteConstanza = obtenerOCrearClienteDemo(userConstanza, "+56970001003", "17111113-9", LocalDate.of(1994, 10, 3), "Piel sensible; consultar antes de usar productos nuevos");
        Cliente clienteFernanda = obtenerOCrearClienteDemo(userFernanda, "+56970001004", "17111114-7", LocalDate.of(1999, 4, 22), "Prefiere atención después del mediodía");
        Cliente clienteSofia = obtenerOCrearClienteDemo(userSofia, "+56970001005", "17111115-5", LocalDate.of(1996, 12, 11), "Sin preferencias registradas");

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

        Trabajador trabajadorFrancisca = obtenerOCrearTrabajadorDemo(userFrancisca, "16111111-1", "+56970002001", "Colorista y estilista", "Especialista en color, balayage y cuidado capilar.", List.of(sCorte, sBalayage));
        Trabajador trabajadorValeria = obtenerOCrearTrabajadorDemo(userValeria, "16111112-0", "+56970002002", "Especialista en uñas", "Manicura y pedicure con técnicas de cuidado personalizado.", List.of(sManicura, sPedicure));
        Trabajador trabajadorDaniela = obtenerOCrearTrabajadorDemo(userDaniela, "16111113-9", "+56970002003", "Esteticista integral", "Tratamientos faciales, pestañas y bienestar.", List.of(sFacial, sCejas, sPestanas, sMasaje));

        // Distribuye el catálogo completo entre los cuatro profesionales.
        agregarServicios(trabajadorCamila, List.of(sCorte, sManicura, sFacial, sCejas));
        agregarServicios(trabajadorFrancisca, List.of(sCorte, sBalayage));
        agregarServicios(trabajadorValeria, List.of(sManicura, sPedicure));
        agregarServicios(trabajadorDaniela, List.of(sFacial, sCejas, sPestanas, sMasaje));

        // Horarios de atención de lunes a viernes
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

        asegurarHorariosBase(trabajadorFrancisca);
        asegurarHorariosBase(trabajadorValeria);
        asegurarHorariosBase(trabajadorDaniela);

        // Citas iniciales de prueba
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

        LocalDate proximaSemana = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).plusWeeks(1);
        LocalDate semanaAnterior = proximaSemana.minusWeeks(2);
        // Historial para que los perfiles y paneles de trabajadores tengan atenciones previas.
        crearCitaDemo("RES-DEMO-0001", clienteValentina, trabajadorValeria, sManicura, semanaAnterior.atTime(10, 0), EstadoCita.COMPLETADA, "Manicura permanente", "Se realizó limpieza de cutículas y esmaltado nude.");
        crearCitaDemo("RES-DEMO-0002", clienteAntonia, trabajadorFrancisca, sCorte, semanaAnterior.plusDays(1).atTime(11, 0), EstadoCita.COMPLETADA, "Corte y brushing", "Corte de puntas y brushing con protector térmico.");
        crearCitaDemo("RES-DEMO-0003", clienteConstanza, trabajadorDaniela, sFacial, semanaAnterior.plusDays(2).atTime(15, 0), EstadoCita.COMPLETADA, "Tratamiento facial", "Limpieza e hidratación; se usaron productos para piel sensible.");
        crearCitaDemo("RES-DEMO-0004", clienteFernanda, trabajadorCamila, sCejas, semanaAnterior.plusDays(3).atTime(12, 0), EstadoCita.COMPLETADA, "Perfilado de cejas", "Diseño y perfilado según preferencia de la clienta.");

        // Reservas futuras de ejemplo distribuidas entre profesionales y servicios.
        crearCitaDemo("RES-DEMO-0005", clienteSofia, trabajadorFrancisca, sBalayage, proximaSemana.atTime(9, 0), EstadoCita.CONFIRMADA, "Coloración y balayage", null);
        crearCitaDemo("RES-DEMO-0006", clienteGale, trabajadorValeria, sPedicure, proximaSemana.atTime(10, 0), EstadoCita.PENDIENTE, "Pedicure spa", null);
        crearCitaDemo("RES-DEMO-0007", clienteMartina, trabajadorDaniela, sPestanas, proximaSemana.plusDays(1).atTime(10, 0), EstadoCita.CONFIRMADA, "Lifting de pestañas", null);
        crearCitaDemo("RES-DEMO-0008", clienteValentina, trabajadorCamila, sCorte, proximaSemana.plusDays(1).atTime(11, 0), EstadoCita.PENDIENTE, "Corte de cabello", null);
        crearCitaDemo("RES-DEMO-0009", clienteAntonia, trabajadorValeria, sManicura, proximaSemana.plusDays(2).atTime(9, 30), EstadoCita.CONFIRMADA, "Manicura permanente", null);
        crearCitaDemo("RES-DEMO-0010", clienteConstanza, trabajadorDaniela, sMasaje, proximaSemana.plusDays(3).atTime(15, 0), EstadoCita.PENDIENTE, "Masaje relajante", null);

        perfilUsuarioService.crearPerfilesFaltantes();
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

    private Cliente obtenerOCrearClienteDemo(Usuario usuario, String telefono, String rut, LocalDate nacimiento, String preferencias) {
        return clienteRepository.findByUsuarioId(usuario.getId()).orElseGet(() -> {
            Cliente cliente = new Cliente();
            cliente.setUsuario(usuario);
            cliente.setTelefono(telefono.trim());
            cliente.setRutDni(rut);
            cliente.setFechaNacimiento(nacimiento);
            cliente.setNotasPreferencias(preferencias);
            cliente.setActivo(true);
            return clienteRepository.save(cliente);
        });
    }

    private Trabajador obtenerOCrearTrabajadorDemo(Usuario usuario, String rut, String telefono, String especialidad, String biografia, List<Servicio> servicios) {
        Trabajador trabajador = trabajadorRepository.findByUsuarioId(usuario.getId()).orElseGet(() -> {
            Trabajador nuevo = new Trabajador();
            nuevo.setUsuario(usuario);
            nuevo.setRutDni(rut);
            nuevo.setTelefono(telefono);
            nuevo.setCargoEspecialidad(especialidad);
            nuevo.setBiografia(biografia);
            nuevo.setComisionPorcentaje(new BigDecimal("30.00"));
            nuevo.setActivo(true);
            nuevo.setServicios(new HashSet<>(servicios));
            return trabajadorRepository.save(nuevo);
        });
        agregarServicios(trabajador, servicios);
        return trabajador;
    }

    private void agregarServicios(Trabajador trabajador, List<Servicio> servicios) {
        Set<Servicio> actuales = trabajador.getServicios() == null ? new HashSet<>() : new HashSet<>(trabajador.getServicios());
        if (actuales.addAll(servicios)) {
            trabajador.setServicios(actuales);
            trabajadorRepository.save(trabajador);
        }
    }

    private void asegurarHorariosBase(Trabajador trabajador) {
        for (int dia = 1; dia <= 5; dia++) {
            if (horarioDisponibilidadRepository.findByTrabajadorIdAndDiaSemanaAndActivoTrue(trabajador.getId(), dia).isEmpty()) {
                HorarioDisponibilidad horario = new HorarioDisponibilidad();
                horario.setTrabajador(trabajador);
                horario.setDiaSemana(dia);
                horario.setHoraInicio(LocalTime.of(9, 0));
                horario.setHoraFin(LocalTime.of(18, 0));
                horario.setHoraInicioDescanso(LocalTime.of(13, 0));
                horario.setHoraFinDescanso(LocalTime.of(14, 0));
                horario.setActivo(true);
                horarioDisponibilidadRepository.save(horario);
            }
        }
    }

    private void crearCitaDemo(String codigo, Cliente cliente, Trabajador trabajador, Servicio servicio,
                               LocalDateTime inicio, EstadoCita estado, String notasCliente, String notasTrabajador) {
        if (citaRepository.findByCodigoReserva(codigo).isPresent()) {
            return;
        }
        Cita cita = new Cita();
        cita.setCodigoReserva(codigo);
        cita.setCliente(cliente);
        cita.setTrabajador(trabajador);
        cita.setServicio(servicio);
        cita.setFechaHoraInicio(inicio);
        cita.setFechaHoraFin(inicio.plusMinutes(servicio.getDuracionMinutos()));
        cita.setEstado(estado);
        cita.setPrecioFinal(servicio.getPrecio());
        cita.setNotasCliente(notasCliente);
        cita.setNotasTrabajador(notasTrabajador);
        citaRepository.save(cita);
    }
}
