package cl.nicolet.backend.service;

import cl.nicolet.backend.model.*;
import cl.nicolet.backend.repository.*;
import cl.nicolet.backend.security.JwtUtils;
import cl.nicolet.backend.security.UserDetailsImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reglas de reserva con la pila real (seguridad, servicios, JPA). La prueba de concurrencia usa hilos con
 * transacciones independientes; hoy corre contra H2 y debe repetirse contra el motor definitivo (MySQL).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReservasIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TipoUsuarioRepository tipoUsuarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private TrabajadorRepository trabajadorRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private HorarioDisponibilidadRepository horarioRepository;
    @Autowired private CitaRepository citaRepository;

    private String sufijo;
    private Usuario admin;
    private Servicio servicio;     // 60 min
    private Servicio otroServicio; // 30 min, NO lo ofrece el profesional A
    private Trabajador proA, proB;
    private final List<Cliente> clientes = new ArrayList<>();
    private LocalDate lunes;

    @BeforeEach
    void fixtures() {
        sufijo = UUID.randomUUID().toString().substring(0, 8);
        lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(1);
        admin = usuario("admin", "ADMINISTRADOR");
        servicio = servicio("Servicio A " + sufijo, 60);
        otroServicio = servicio("Servicio B " + sufijo, 30);
        proA = trabajador("proA", Set.of(servicio));
        proB = trabajador("proB", Set.of(servicio));
        for (int i = 0; i < 8; i++) clientes.add(cliente("cli" + i));
    }

    @AfterEach
    void limpiar() {
        List<Trabajador> pros = List.of(proA, proB);
        for (Trabajador t : pros) {
            citaRepository.deleteAll(citaRepository.findByTrabajadorId(t.getId()));
            horarioRepository.deleteAll(horarioRepository.findByTrabajadorId(t.getId()));
        }
        trabajadorRepository.deleteAll(pros);
        clienteRepository.deleteAll(clientes);
        servicioRepository.deleteAll(List.of(servicio, otroServicio));
        for (Usuario u : new ArrayList<>(usuarioRepository.findAll())) {
            if (u.getCorreo().endsWith("@reservas." + sufijo + ".test")) usuarioRepository.delete(u);
        }
    }

    // ------------------------------------------------------------ fixtures

    private Usuario usuario(String alias, String rol) {
        Usuario u = new Usuario();
        u.setNombre(alias);
        u.setApellidoP("Res");
        u.setCorreo(alias + "@reservas." + sufijo + ".test");
        u.setPassword(passwordEncoder.encode("Clave-Segura-123"));
        u.setTipoUsuario(tipoUsuarioRepository.findByNombreIgnoreCase(rol).orElseThrow());
        return usuarioRepository.save(u);
    }

    private Servicio servicio(String nombre, int minutos) {
        Servicio s = new Servicio();
        s.setNombre(nombre);
        s.setDuracionMinutos(minutos);
        s.setPrecio(new BigDecimal("10000.00"));
        s.setActivo(true);
        return servicioRepository.save(s);
    }

    private Trabajador trabajador(String alias, Set<Servicio> servicios) {
        Trabajador t = new Trabajador();
        t.setUsuario(usuario(alias, "PROFESIONAL"));
        t.setCargoEspecialidad("Estilista");
        t.setActivo(true);
        t.setServicios(new HashSet<>(servicios));
        t = trabajadorRepository.save(t);
        for (int dia = 1; dia <= 7; dia++) {
            HorarioDisponibilidad h = new HorarioDisponibilidad();
            h.setTrabajador(t);
            h.setDiaSemana(dia);
            h.setHoraInicio(LocalTime.of(9, 0));
            h.setHoraFin(LocalTime.of(18, 0));
            h.setActivo(true);
            horarioRepository.save(h);
        }
        return t;
    }

    private Cliente cliente(String alias) {
        Cliente c = new Cliente();
        c.setUsuario(usuario(alias, "CLIENTE"));
        c.setActivo(true);
        return clienteRepository.save(c);
    }

    private String bearer() {
        return "Bearer " + jwtUtils.generarToken(UserDetailsImpl.build(admin));
    }

    private int reservar(Cliente c, Trabajador t, Servicio s, LocalDateTime inicio) throws Exception {
        return reservarResultado(c, t, s, inicio).getResponse().getStatus();
    }

    private MvcResult reservarResultado(Cliente c, Trabajador t, Servicio s, LocalDateTime inicio) throws Exception {
        String body = "{\"clienteId\":" + c.getId() + ",\"trabajadorId\":" + t.getId() + ",\"servicioId\":" + s.getId()
                + ",\"fechaHoraInicio\":\"" + inicio + "\"}";
        return mockMvc.perform(post("/api/v2/nicolet/citas").header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
    }

    private long patchEstado(long citaId, String estado, String motivo) throws Exception {
        String body = "{\"nuevoEstado\":\"" + estado + "\"" + (motivo == null ? "" : ",\"motivoONotas\":\"" + motivo + "\"") + "}";
        return mockMvc.perform(patch("/api/v2/nicolet/citas/" + citaId + "/estado").header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus();
    }

    private List<Integer> enParalelo(List<Callable<Integer>> tareas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tareas.size());
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Integer>> futuros = new ArrayList<>();
        for (Callable<Integer> tarea : tareas) {
            futuros.add(pool.submit(() -> { salida.await(); return tarea.call(); }));
        }
        salida.countDown(); // todas las solicitudes arrancan a la vez
        List<Integer> estados = new ArrayList<>();
        for (Future<Integer> f : futuros) estados.add(f.get(60, TimeUnit.SECONDS));
        pool.shutdown();
        return estados;
    }

    private long contar(List<Integer> estados, int codigo) {
        return estados.stream().filter(e -> e == codigo).count();
    }

    // ------------------------------------------------------------------ E07

    @Test
    @DisplayName("E07: 8 solicitudes simultáneas por el mismo intervalo → solo una reserva")
    void intervalosIdenticosEnParalelo() throws Exception {
        LocalDateTime inicio = lunes.atTime(10, 0);
        List<Callable<Integer>> tareas = new ArrayList<>();
        for (Cliente c : clientes) tareas.add(() -> reservar(c, proA, servicio, inicio));

        List<Integer> estados = enParalelo(tareas);

        assertThat(contar(estados, 201)).as("estados=%s", estados).isEqualTo(1);
        assertThat(contar(estados, 400)).isEqualTo(clientes.size() - 1);
        assertThat(citaRepository.findByTrabajadorId(proA.getId())).hasSize(1);
    }

    @Test
    @DisplayName("E07: intervalos con inicios distintos pero superpuestos (10:00, 10:15, 10:30) → solo uno; 11:00 contiguo cabe")
    void parcialmenteSuperpuestosYContiguos() throws Exception {
        List<Callable<Integer>> tareas = new ArrayList<>();
        int[] minutos = {0, 15, 30, 45};                 // 10:00-11:00, 10:15-11:15, 10:30-11:30, 10:45-11:45: todos se cruzan
        for (int i = 0; i < minutos.length; i++) {
            Cliente c = clientes.get(i);
            LocalDateTime inicio = lunes.atTime(10, minutos[i]);
            tareas.add(() -> reservar(c, proA, servicio, inicio));
        }
        List<Integer> estados = enParalelo(tareas);
        assertThat(contar(estados, 201)).as("estados=%s", estados).isEqualTo(1);

        // Contiguo: empieza justo cuando termina la reserva aceptada → se acepta, sin solapamiento
        Cita aceptada = citaRepository.findByTrabajadorId(proA.getId()).get(0);
        assertThat(reservar(clientes.get(5), proA, servicio, aceptada.getFechaHoraFin())).isEqualTo(201);
    }

    @Test
    @DisplayName("E07: profesionales distintos reservan en paralelo el mismo horario sin bloquearse entre sí")
    void profesionalesDistintosNoSeSerializan() throws Exception {
        LocalDateTime inicio = lunes.atTime(15, 0);
        List<Callable<Integer>> tareas = List.of(
                () -> reservar(clientes.get(0), proA, servicio, inicio),
                () -> reservar(clientes.get(1), proB, servicio, inicio));
        List<Integer> estados = enParalelo(tareas);
        assertThat(estados).containsExactly(201, 201);
    }

    // ------------------------------------------------------------------ E08

    @Test
    @DisplayName("E08: una cita cancelada no puede reactivarse sobre otra reserva que tomó su horario")
    void canceladaNoSeReactiva() throws Exception {
        LocalDateTime inicio = lunes.atTime(11, 0);
        MvcResult a = reservarResultado(clientes.get(0), proA, servicio, inicio);
        long idA = mapper.readTree(a.getResponse().getContentAsString()).get("id").asLong();

        assertThat(patchEstado(idA, "CANCELADA", "Cliente cancela")).isEqualTo(200);
        assertThat(reservar(clientes.get(1), proA, servicio, inicio)).as("B toma el horario liberado").isEqualTo(201);

        assertThat(patchEstado(idA, "CONFIRMADA", null)).as("reactivar A").isEqualTo(400);
        assertThat(patchEstado(idA, "PENDIENTE", null)).isEqualTo(400);
        assertThat(citaRepository.findById(idA).orElseThrow().getEstado()).isEqualTo(EstadoCita.CANCELADA);
        // el horario sigue ocupado una sola vez
        assertThat(citaRepository.findByTrabajadorId(proA.getId()).stream()
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("E08: transiciones legales e ilegales (PENDIENTE→CONFIRMADA→EN_ATENCION→COMPLETADA; finales inmutables)")
    void transiciones() throws Exception {
        long id = mapper.readTree(reservarResultado(clientes.get(0), proA, servicio, lunes.atTime(9, 0))
                .getResponse().getContentAsString()).get("id").asLong();
        assertThat(patchEstado(id, "COMPLETADA", "x")).as("PENDIENTE→COMPLETADA").isEqualTo(400);
        assertThat(patchEstado(id, "EN_ATENCION", null)).as("PENDIENTE→EN_ATENCION").isEqualTo(400);
        assertThat(patchEstado(id, "CONFIRMADA", null)).isEqualTo(200);
        assertThat(patchEstado(id, "CONFIRMADA", null)).as("mismo estado").isEqualTo(400);
        assertThat(patchEstado(id, "EN_ATENCION", null)).isEqualTo(200);
        assertThat(patchEstado(id, "COMPLETADA", "Trabajo realizado")).isEqualTo(200);
        for (String destino : new String[]{"PENDIENTE", "CONFIRMADA", "CANCELADA", "NO_ASISTIO"}) {
            assertThat(patchEstado(id, destino, "motivo")).as("COMPLETADA→" + destino).isEqualTo(400);
        }
        // cancelar exige motivo
        long id2 = mapper.readTree(reservarResultado(clientes.get(1), proA, servicio, lunes.atTime(12, 0))
                .getResponse().getContentAsString()).get("id").asLong();
        assertThat(patchEstado(id2, "CANCELADA", null)).isEqualTo(400);
        assertThat(patchEstado(id2, "CANCELADA", "x".repeat(256))).isEqualTo(400);
        assertThat(patchEstado(id2, "CANCELADA", "x".repeat(255))).isEqualTo(200);
    }

    // ------------------------------------------------------------------ E09

    @Test
    @DisplayName("E09: reserva y disponibilidad rechazan un servicio que el profesional no ofrece")
    void servicioNoOfrecido() throws Exception {
        assertThat(reservar(clientes.get(0), proA, otroServicio, lunes.atTime(10, 0))).isEqualTo(400);
        assertThat(citaRepository.findByTrabajadorId(proA.getId())).isEmpty();
        mockMvc.perform(get("/api/v2/nicolet/disponibilidad").param("trabajadorId", String.valueOf(proA.getId()))
                        .param("fecha", lunes.toString()).param("servicioId", String.valueOf(otroServicio.getId())))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v2/nicolet/disponibilidad").param("trabajadorId", String.valueOf(proA.getId()))
                        .param("fecha", lunes.toString()).param("servicioId", String.valueOf(servicio.getId())))
                .andExpect(status().isOk());
        // servicio inactivo
        servicio.setActivo(false);
        servicioRepository.save(servicio);
        assertThat(reservar(clientes.get(0), proA, servicio, lunes.atTime(10, 0))).isEqualTo(400);
    }

    // ------------------------------------------------------------------ E10

    private int putTrabajador(String serviciosJson) throws Exception {
        String body = "{\"usuarioId\":" + proA.getUsuario().getId() + ",\"cargoEspecialidad\":\"Estilista\""
                + (serviciosJson == null ? "" : ",\"servicioIds\":" + serviciosJson) + "}";
        return mockMvc.perform(put("/api/v2/nicolet/trabajadores/" + proA.getId()).header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus();
    }

    private Set<Long> serviciosDeA() {
        return trabajadorRepository.findById(proA.getId()).orElseThrow().getServicios()
                .stream().map(Servicio::getId).collect(java.util.stream.Collectors.toSet());
    }

    @Test
    @DisplayName("E10: servicioIds null = sin cambio; [] = quitar todos; IDs inexistentes → 400 sin cambios; válidos reemplazan")
    void edicionDeServiciosDelProfesional() throws Exception {
        assertThat(serviciosDeA()).containsExactly(servicio.getId());

        assertThat(putTrabajador(null)).isEqualTo(200);
        assertThat(serviciosDeA()).as("null = sin cambio").containsExactly(servicio.getId());

        assertThat(putTrabajador("[" + servicio.getId() + ",999999]")).as("ID inexistente").isEqualTo(400);
        assertThat(serviciosDeA()).as("sin cambios tras rechazo").containsExactly(servicio.getId());

        assertThat(putTrabajador("[" + servicio.getId() + "," + otroServicio.getId() + "]")).isEqualTo(200);
        assertThat(serviciosDeA()).containsExactlyInAnyOrder(servicio.getId(), otroServicio.getId());

        assertThat(putTrabajador("[]")).as("[] quita todos").isEqualTo(200);
        assertThat(serviciosDeA()).isEmpty();
    }

    // ------------------------------------------------------------------ E11 / E12

    private String semana(String lunesJson) {
        StringBuilder sb = new StringBuilder("[").append(lunesJson);
        for (int d = 2; d <= 7; d++) {
            sb.append(",{\"diaSemana\":").append(d).append(",\"horaInicio\":\"09:00\",\"horaFin\":\"18:00\",\"activo\":true}");
        }
        return sb.append("]").toString();
    }

    @Test
    @DisplayName("E11: una semana con un elemento null → 400 y ninguna modificación")
    void semanaConElementoNull() throws Exception {
        var antes = horarioRepository.findByTrabajadorId(proA.getId()).stream()
                .map(h -> h.getDiaSemana() + "-" + h.getHoraInicio() + "-" + h.getHoraFin()).sorted().toList();
        mockMvc.perform(put("/api/v2/nicolet/horarios/trabajador/" + proA.getId()).header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(semana("null")))
                .andExpect(status().isBadRequest());
        var despues = horarioRepository.findByTrabajadorId(proA.getId()).stream()
                .map(h -> h.getDiaSemana() + "-" + h.getHoraInicio() + "-" + h.getHoraFin()).sorted().toList();
        assertThat(despues).isEqualTo(antes);
    }

    @Test
    @DisplayName("E12: cambiar la jornada no borra citas; se informan en X-Citas-Fuera-De-Jornada y siguen bloqueando")
    void cambioDeJornadaConCitasExistentes() throws Exception {
        assertThat(reservar(clientes.get(0), proA, servicio, lunes.atTime(9, 0))).isEqualTo(201); // 09:00-10:00
        String nuevaSemana = semana("{\"diaSemana\":1,\"horaInicio\":\"09:30\",\"horaFin\":\"18:00\",\"activo\":true}");

        MvcResult r = mockMvc.perform(put("/api/v2/nicolet/horarios/trabajador/" + proA.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer()).contentType(MediaType.APPLICATION_JSON).content(nuevaSemana))
                .andExpect(status().isOk()).andReturn();
        assertThat(r.getResponse().getHeader("X-Citas-Fuera-De-Jornada")).isEqualTo("1");
        assertThat(citaRepository.findByTrabajadorId(proA.getId())).as("la cita se conserva").hasSize(1);

        // La disponibilidad del lunes no ofrece 09:30 (la cita 09:00-10:00 lo ocupa) pero sí 10:00
        MvcResult disp = mockMvc.perform(get("/api/v2/nicolet/disponibilidad")
                        .param("trabajadorId", String.valueOf(proA.getId())).param("fecha", lunes.toString())
                        .param("servicioId", String.valueOf(servicio.getId()))).andExpect(status().isOk()).andReturn();
        JsonNode bloques = mapper.readTree(disp.getResponse().getContentAsString());
        assertThat(bloques.get(0).get("horaInicio").asText()).startsWith("09:30");
        assertThat(bloques.get(0).get("disponible").asBoolean()).isFalse();
        // y la reserva de ese mismo bloque también se rechaza: consulta y alta coinciden
        assertThat(reservar(clientes.get(1), proA, servicio, lunes.atTime(9, 30))).isEqualTo(400);
        assertThat(reservar(clientes.get(1), proA, servicio, lunes.atTime(10, 0))).isEqualTo(201);
    }

    // ------------------------------------------------------------------ E13

    @Test
    @DisplayName("E13: disponibilidad de ayer → 400; hoy y mañana → 200")
    void fechasPasadas() throws Exception {
        for (long delta : new long[]{-1, 0, 1}) {
            var req = get("/api/v2/nicolet/disponibilidad").param("trabajadorId", String.valueOf(proA.getId()))
                    .param("fecha", LocalDate.now().plusDays(delta).toString());
            if (delta < 0) mockMvc.perform(req).andExpect(status().isBadRequest());
            else mockMvc.perform(req).andExpect(status().isOk());
        }
    }
}
