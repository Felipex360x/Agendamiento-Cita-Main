package cl.nicolet.backend.security;

import cl.nicolet.backend.model.Trabajador;
import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.repository.ClienteRepository;
import cl.nicolet.backend.repository.HorarioDisponibilidadRepository;
import cl.nicolet.backend.repository.TipoUsuarioRepository;
import cl.nicolet.backend.repository.TrabajadorRepository;
import cl.nicolet.backend.repository.UsuarioRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** E06: «Sin rol» (tipoUsuarioId:null explícito) retira el rol; omitir el campo lo conserva. */
@SpringBootTest
@AutoConfigureMockMvc
class RetiroDeRolIntegrationTest {

    private static final String PASSWORD = "Clave-Segura-123";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TipoUsuarioRepository tipoUsuarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private TrabajadorRepository trabajadorRepository;
    @Autowired private HorarioDisponibilidadRepository horarioRepository;

    private final List<Usuario> usuarios = new ArrayList<>();
    private Usuario admin;
    private String sufijo;

    @BeforeEach
    void setUp() {
        sufijo = UUID.randomUUID().toString().substring(0, 8);
        admin = usuario("admin", "ADMINISTRADOR");
    }

    @AfterEach
    void limpiar() {
        for (Usuario u : usuarios) {
            trabajadorRepository.findByUsuarioId(u.getId()).ifPresent(t -> {
                horarioRepository.deleteAll(horarioRepository.findByTrabajadorId(t.getId()));
                trabajadorRepository.delete(t);
            });
            clienteRepository.findByUsuarioId(u.getId()).ifPresent(clienteRepository::delete);
        }
        usuarioRepository.deleteAll(usuarios);
    }

    private Usuario usuario(String alias, String rol) {
        Usuario u = new Usuario();
        u.setNombre(alias);
        u.setApellidoP("Rol");
        u.setCorreo(alias + "." + sufijo + "@rol.test");
        u.setPassword(passwordEncoder.encode(PASSWORD));
        u.setTipoUsuario(rol == null ? null : tipoUsuarioRepository.findByNombreIgnoreCase(rol).orElseThrow());
        u = usuarioRepository.save(u);
        usuarios.add(u);
        return u;
    }

    private String bearer(Usuario u) {
        return "Bearer " + jwtUtils.generarToken(UserDetailsImpl.build(usuarioRepository.findById(u.getId()).orElseThrow()));
    }

    private String cuerpo(Usuario u, String tipoUsuarioIdJson) {
        String rol = tipoUsuarioIdJson == null ? "" : ",\"tipoUsuarioId\":" + tipoUsuarioIdJson;
        return "{\"nombre\":\"" + u.getNombre() + "\",\"apellidoP\":\"Rol\",\"correo\":\"" + u.getCorreo() + "\"" + rol + "}";
    }

    private void actualizarComoAdmin(Usuario objetivo, String tipoUsuarioIdJson) throws Exception {
        mockMvc.perform(put("/api/v2/nicolet/usuarios/" + objetivo.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(objetivo, tipoUsuarioIdJson)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Retirar el rol ADMINISTRADOR con null explícito: se persiste y la siguiente petición pierde permisos")
    void retirarRolAdministrador() throws Exception {
        Usuario otroAdmin = usuario("otroAdmin", "ADMINISTRADOR");
        String tokenAnterior = bearer(otroAdmin);
        mockMvc.perform(get("/api/v2/nicolet/usuarios").header(HttpHeaders.AUTHORIZATION, tokenAnterior))
                .andExpect(status().isOk());

        actualizarComoAdmin(otroAdmin, "null");

        assertThat(usuarioRepository.findById(otroAdmin.getId()).orElseThrow().getTipoUsuario()).isNull();
        // con el MISMO token: sin permisos de administrador ni de ningún otro tipo
        mockMvc.perform(get("/api/v2/nicolet/usuarios").header(HttpHeaders.AUTHORIZATION, tokenAnterior))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v2/nicolet/citas").header(HttpHeaders.AUTHORIZATION, tokenAnterior))
                .andExpect(status().isForbidden());
        // /me sigue disponible y NO le atribuye CLIENTE
        mockMvc.perform(get("/api/v2/nicolet/auth/me").header(HttpHeaders.AUTHORIZATION, tokenAnterior))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("SIN_ROL"));
        // y no puede iniciar sesión de nuevo: credenciales válidas, sin rol -> 403 y sin token
        mockMvc.perform(post("/api/v2/nicolet/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + otroAdmin.getCorreo() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("Omitir tipoUsuarioId conserva el rol (no es una retirada)")
    void omitirNoRetira() throws Exception {
        Usuario otroAdmin = usuario("conservaRol", "ADMINISTRADOR");
        actualizarComoAdmin(otroAdmin, null);
        assertThat(usuarioRepository.findById(otroAdmin.getId()).orElseThrow().getTipoUsuario().getNombre())
                .isEqualTo("ADMINISTRADOR");
        mockMvc.perform(get("/api/v2/nicolet/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(otroAdmin)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Cambiar de rol sigue funcionando (id explícito)")
    void cambiarRol() throws Exception {
        Usuario u = usuario("cambia", "ADMINISTRADOR");
        Long idCliente = tipoUsuarioRepository.findByNombreIgnoreCase("CLIENTE").orElseThrow().getId();
        actualizarComoAdmin(u, String.valueOf(idCliente));
        assertThat(usuarioRepository.findById(u.getId()).orElseThrow().getTipoUsuario().getNombre()).isEqualTo("CLIENTE");
        mockMvc.perform(get("/api/v2/nicolet/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(u)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Retirar el rol desactiva los perfiles de profesional y de cliente sin borrarlos")
    void retirarRolDesactivaPerfiles() throws Exception {
        Usuario pro = usuario("pro", "PROFESIONAL");
        actualizarComoAdmin(pro, String.valueOf(pro.getTipoUsuario().getId())); // sincroniza: crea el perfil
        Trabajador t = trabajadorRepository.findByUsuarioId(pro.getId()).orElseThrow();
        assertThat(t.getActivo()).isTrue();

        actualizarComoAdmin(pro, "null");

        Trabajador despues = trabajadorRepository.findByUsuarioId(pro.getId()).orElseThrow();
        assertThat(despues.getActivo()).isFalse();           // fuera del directorio y de la disponibilidad
        assertThat(despues.getId()).isEqualTo(t.getId());    // el perfil y su historial se conservan

        Usuario cli = usuario("cli", "CLIENTE");
        actualizarComoAdmin(cli, String.valueOf(cli.getTipoUsuario().getId()));
        actualizarComoAdmin(cli, "null");
        assertThat(clienteRepository.findByUsuarioId(cli.getId()).orElseThrow().getActivo()).isFalse();
    }
}
