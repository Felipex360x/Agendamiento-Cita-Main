package cl.nicolet.backend.security;

import cl.nicolet.backend.model.Cita;
import cl.nicolet.backend.model.Cliente;
import cl.nicolet.backend.model.EstadoCita;
import cl.nicolet.backend.model.Servicio;
import cl.nicolet.backend.model.Trabajador;
import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.repository.CitaRepository;
import cl.nicolet.backend.repository.ClienteRepository;
import cl.nicolet.backend.repository.ServicioRepository;
import cl.nicolet.backend.repository.TipoUsuarioRepository;
import cl.nicolet.backend.repository.TrabajadorRepository;
import cl.nicolet.backend.repository.UsuarioRepository;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriz de autorización con la cadena de seguridad real (sin filtros desactivados) para
 * ADMINISTRADOR, CLIENTE, PROFESIONAL, propietario, tercero y usuario sin autenticar.
 * Incluye E02: un cliente no recibe RUT, teléfono, correo ni comisión del profesional.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationIntegrationTest {

    private static final String RUT_PRO = "11111111-1";
    private static final String TEL_PRO = "+56911112222";
    private static final String COMISION_PRO = "37.50";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TipoUsuarioRepository tipoUsuarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private TrabajadorRepository trabajadorRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private CitaRepository citaRepository;

    private Usuario admin, usuarioClienteA, usuarioClienteB, usuarioProA, usuarioProB;
    private Cliente clienteA, clienteB;
    private Trabajador proA, proB;
    private Servicio servicio;
    private Cita cita;
    private String sufijo;

    @BeforeEach
    void fixtures() {
        sufijo = UUID.randomUUID().toString().substring(0, 8);
        admin = usuario("admin", "ADMINISTRADOR");
        usuarioClienteA = usuario("clienteA", "CLIENTE");
        usuarioClienteB = usuario("clienteB", "CLIENTE");
        usuarioProA = usuario("proA", "PROFESIONAL");
        usuarioProB = usuario("proB", "PROFESIONAL");

        clienteA = cliente(usuarioClienteA, "9" + sufijo.substring(0, 3) + "1-1");
        clienteB = cliente(usuarioClienteB, "9" + sufijo.substring(0, 3) + "2-2");
        proA = trabajador(usuarioProA, RUT_PRO + sufijo.substring(0, 1), TEL_PRO, new BigDecimal(COMISION_PRO));
        proB = trabajador(usuarioProB, "22222222-" + sufijo.substring(0, 2), "+56933334444", new BigDecimal("10.00"));

        servicio = new Servicio();
        servicio.setNombre("Servicio authz " + sufijo);
        servicio.setDuracionMinutos(30);
        servicio.setPrecio(new BigDecimal("10000.00"));
        servicio.setActivo(true);
        servicio = servicioRepository.save(servicio);

        cita = new Cita();
        cita.setCodigoReserva("RES-9999-" + sufijo.substring(0, 6).toUpperCase());
        cita.setCliente(clienteA);
        cita.setTrabajador(proA);
        cita.setServicio(servicio);
        cita.setFechaHoraInicio(LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0));
        cita.setFechaHoraFin(cita.getFechaHoraInicio().plusMinutes(30));
        cita.setEstado(EstadoCita.PENDIENTE);
        cita.setPrecioFinal(new BigDecimal("10000.00"));
        cita = citaRepository.save(cita);
    }

    @AfterEach
    void limpiar() {
        citaRepository.deleteById(cita.getId());
        trabajadorRepository.deleteAll(java.util.List.of(proA, proB));
        clienteRepository.deleteAll(java.util.List.of(clienteA, clienteB));
        servicioRepository.deleteById(servicio.getId());
        usuarioRepository.deleteAll(java.util.List.of(admin, usuarioClienteA, usuarioClienteB, usuarioProA, usuarioProB));
    }

    private Usuario usuario(String alias, String rol) {
        Usuario u = new Usuario();
        u.setNombre(alias);
        u.setApellidoP("Authz");
        u.setCorreo(alias + "." + sufijo + "@authz.test");
        u.setPassword(passwordEncoder.encode("Clave-Segura-123"));
        u.setTipoUsuario(tipoUsuarioRepository.findByNombreIgnoreCase(rol).orElseThrow());
        return usuarioRepository.save(u);
    }

    private Cliente cliente(Usuario u, String rut) {
        Cliente c = new Cliente();
        c.setUsuario(u);
        c.setRutDni(rut);
        c.setTelefono("+5690000" + rut.substring(0, 4));
        c.setActivo(true);
        return clienteRepository.save(c);
    }

    private Trabajador trabajador(Usuario u, String rut, String tel, BigDecimal comision) {
        Trabajador t = new Trabajador();
        t.setUsuario(u);
        t.setRutDni(rut);
        t.setTelefono(tel);
        t.setCargoEspecialidad("Estilista");
        t.setComisionPorcentaje(comision);
        t.setActivo(true);
        return trabajadorRepository.save(t);
    }

    private String token(Usuario u) {
        return "Bearer " + jwtUtils.generarToken(UserDetailsImpl.build(u));
    }

    private ResultActions como(Usuario u, MockHttpServletRequestBuilder req) throws Exception {
        if (u != null) {
            req.header(HttpHeaders.AUTHORIZATION, token(u));
        }
        return mockMvc.perform(req);
    }

    // ------------------------------------------------------------------ matriz

    @Test
    @DisplayName("GET /citas (listado global): solo ADMINISTRADOR")
    void listadoGlobalSoloAdmin() throws Exception {
        como(null, get("/api/v2/nicolet/citas")).andExpect(status().isUnauthorized());
        como(usuarioClienteA, get("/api/v2/nicolet/citas")).andExpect(status().isForbidden());
        como(usuarioProA, get("/api/v2/nicolet/citas")).andExpect(status().isForbidden());
        como(admin, get("/api/v2/nicolet/citas")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /citas/{id} y /citas/codigo/{codigo}: participantes y admin; terceros 403; anónimo 401")
    void citaPorIdYCodigo() throws Exception {
        for (String url : new String[]{
                "/api/v2/nicolet/citas/" + cita.getId(),
                "/api/v2/nicolet/citas/codigo/" + cita.getCodigoReserva()}) {
            como(null, get(url)).andExpect(status().isUnauthorized());
            como(usuarioClienteA, get(url)).andExpect(status().isOk());   // propietario
            como(usuarioProA, get(url)).andExpect(status().isOk());       // profesional asignado
            como(admin, get(url)).andExpect(status().isOk());
            como(usuarioClienteB, get(url)).andExpect(status().isForbidden()); // tercero cliente
            como(usuarioProB, get(url)).andExpect(status().isForbidden());     // tercero profesional
        }
    }

    @Test
    @DisplayName("GET /citas/cliente/{id}: propietario y admin; otro cliente o profesional 403")
    void citasPorCliente() throws Exception {
        String url = "/api/v2/nicolet/citas/cliente/" + clienteA.getId();
        como(null, get(url)).andExpect(status().isUnauthorized());
        como(usuarioClienteA, get(url)).andExpect(status().isOk());
        como(admin, get(url)).andExpect(status().isOk());
        como(usuarioClienteB, get(url)).andExpect(status().isForbidden());
        como(usuarioProA, get(url)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /citas/trabajador/{id}: el profesional propietario y admin; cliente y otro profesional 403")
    void citasPorTrabajador() throws Exception {
        String url = "/api/v2/nicolet/citas/trabajador/" + proA.getId();
        como(null, get(url)).andExpect(status().isUnauthorized());
        como(usuarioProA, get(url)).andExpect(status().isOk());
        como(admin, get(url)).andExpect(status().isOk());
        como(usuarioProB, get(url)).andExpect(status().isForbidden());
        como(usuarioClienteA, get(url)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /citas/{id}/estado: el cliente no puede cambiar estados; terceros tampoco")
    void cambioDeEstado() throws Exception {
        String url = "/api/v2/nicolet/citas/" + cita.getId() + "/estado";
        String body = "{\"nuevoEstado\":\"CONFIRMADA\"}";
        como(null, patch(url).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        como(usuarioClienteA, patch(url).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        como(usuarioProB, patch(url).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        como(usuarioProA, patch(url).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Recursos administrativos: usuarios, clientes y perfil de trabajador")
    void recursosAdministrativos() throws Exception {
        for (String url : new String[]{"/api/v2/nicolet/usuarios", "/api/v2/nicolet/clientes", "/api/v2/nicolet/tipos-usuario"}) {
            como(null, get(url)).andExpect(status().isUnauthorized());
            como(usuarioClienteA, get(url)).andExpect(status().isForbidden());
            como(usuarioProA, get(url)).andExpect(status().isForbidden());
            como(admin, get(url)).andExpect(status().isOk());
        }
        String perfilPro = "/api/v2/nicolet/trabajadores/" + proA.getId();
        como(usuarioProA, get(perfilPro)).andExpect(status().isOk());      // propietario
        como(admin, get(perfilPro)).andExpect(status().isOk());
        como(usuarioProB, get(perfilPro)).andExpect(status().isForbidden()); // tercero
        como(usuarioClienteA, get(perfilPro)).andExpect(status().isForbidden());

        String perfilCliente = "/api/v2/nicolet/clientes/" + clienteA.getId();
        como(usuarioClienteA, get(perfilCliente)).andExpect(status().isOk());
        como(usuarioClienteB, get(perfilCliente)).andExpect(status().isForbidden());
        como(admin, get(perfilCliente)).andExpect(status().isOk());
    }

    // --------------------------------------------------------------------- E02

    private void assertSinDatosInternosDelProfesional(String json) throws Exception {
        assertThat(json).doesNotContain(RUT_PRO).doesNotContain(TEL_PRO).doesNotContain(COMISION_PRO)
                .doesNotContain("37.5")
                .doesNotContain(usuarioProA.getCorreo()); // el correo del cliente sí es legítimo; el del profesional no
        JsonNode cita = objectMapper.readTree(json);
        // en el listado la cita es un elemento del array
        JsonNode trabajador = (cita.isArray() ? cita.get(0) : cita).get("trabajador");
        assertThat(trabajador.hasNonNull("rutDni")).isFalse();
        assertThat(trabajador.hasNonNull("telefono")).isFalse();
        assertThat(trabajador.hasNonNull("comisionPorcentaje")).isFalse();
        assertThat(trabajador.get("usuario").hasNonNull("correo")).isFalse();
        // lo que sí necesita la interfaz
        assertThat(trabajador.get("id").asLong()).isEqualTo(proA.getId());
        assertThat(trabajador.get("usuario").get("nombre").asText()).isEqualTo("proA");
        assertThat(trabajador.get("cargoEspecialidad").asText()).isEqualTo("Estilista");
    }

    @Test
    @DisplayName("E02: cliente no recibe RUT, teléfono, correo ni comisión del profesional (id, código y listado propio)")
    void clienteNoRecibeDatosInternosDelProfesional() throws Exception {
        for (String url : new String[]{
                "/api/v2/nicolet/citas/" + cita.getId(),
                "/api/v2/nicolet/citas/codigo/" + cita.getCodigoReserva(),
                "/api/v2/nicolet/citas/cliente/" + clienteA.getId()}) {
            String json = como(usuarioClienteA, get(url)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertSinDatosInternosDelProfesional(json);
        }
    }

    @Test
    @DisplayName("E02: tampoco el cambio de estado devuelve datos internos; el admin los obtiene en /trabajadores/{id}")
    void estadoYPresenciaAdministrativa() throws Exception {
        String json = como(usuarioProA, patch("/api/v2/nicolet/citas/" + cita.getId() + "/estado")
                .contentType(MediaType.APPLICATION_JSON).content("{\"nuevoEstado\":\"CONFIRMADA\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertSinDatosInternosDelProfesional(json);

        // Presencia administrativa permitida: el perfil completo sigue disponible para admin y propietario.
        for (Usuario quien : new Usuario[]{admin, usuarioProA}) {
            JsonNode perfil = objectMapper.readTree(
                    como(quien, get("/api/v2/nicolet/trabajadores/" + proA.getId()))
                            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            assertThat(perfil.get("rutDni").asText()).startsWith(RUT_PRO);
            assertThat(perfil.get("telefono").asText()).isEqualTo(TEL_PRO);
            assertThat(perfil.get("comisionPorcentaje").decimalValue()).isEqualByComparingTo(COMISION_PRO);
        }
    }
}
