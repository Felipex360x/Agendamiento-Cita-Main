package cl.nicolet.backend.security;

import cl.nicolet.backend.repository.ClienteRepository;
import cl.nicolet.backend.repository.UsuarioRepository;
import cl.nicolet.backend.service.PerfilUsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** E03 (registro atómico), E04 (límite de correo), E05 (contraseñas como texto de entrada). */
@SpringBootTest
@AutoConfigureMockMvc
class RegistrationIntegrationTest {

    private static final String REGISTER = "/api/v2/nicolet/auth/register";
    private static final String LOGIN = "/api/v2/nicolet/auth/login";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private LoginAttemptService loginAttemptService;
    @SpyBean private PerfilUsuarioService perfilUsuarioService;

    private final List<String> correos = new ArrayList<>();

    @AfterEach
    void limpiar() {
        reset(perfilUsuarioService);
        for (String c : correos) {
            usuarioRepository.findByCorreoIgnoreCase(c).ifPresent(u -> {
                clienteRepository.findByUsuarioId(u.getId()).ifPresent(clienteRepository::delete);
                usuarioRepository.delete(u);
            });
        }
        loginAttemptService.reset();
    }

    private String correoUnico() {
        String c = "reg." + UUID.randomUUID().toString().substring(0, 8) + "@registro.test";
        correos.add(c);
        return c;
    }

    private ResultActions registrar(String correo, String password, String telefono) throws Exception {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("nombre", "Ana");
        body.put("apellidoP", "Prueba");
        body.put("correo", correo);
        body.put("password", password);
        if (telefono != null) body.put("telefono", telefono);
        if (!correos.contains(correo)) correos.add(correo);
        return mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));
    }

    private ResultActions login(String correo, String password) throws Exception {
        return mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("correo", correo, "password", password))));
    }

    // ------------------------------------------------------------------ E03

    @Test
    @DisplayName("E03: alta válida crea cuenta y perfil, y devuelve sesión utilizable")
    void altaValida() throws Exception {
        String correo = correoUnico();
        String token = mapper.readTree(registrar(correo, "Clave-Valida-1", "+56911112222")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andReturn().getResponse().getContentAsString()).get("token").asText();

        var usuario = usuarioRepository.findByCorreoIgnoreCase(correo).orElseThrow();
        assertThat(clienteRepository.findByUsuarioId(usuario.getId())).isPresent();
        mockMvc.perform(get("/api/v2/nicolet/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.correo").value(correo));
    }

    @Test
    @DisplayName("E03: si falla la creación del perfil no queda cuenta huérfana y el correo sigue libre")
    void fallaPerfilRevierteTodo() throws Exception {
        String correo = correoUnico();
        doThrow(new org.springframework.dao.DataIntegrityViolationException("fallo simulado al guardar el perfil"))
                .when(perfilUsuarioService).crearPerfilCliente(any(), any());

        String respuesta = registrar(correo, "Clave-Valida-1", null).andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();
        assertThat(respuesta).doesNotContain("ya se encuentra registrado"); // no se confunde con correo duplicado

        assertThat(usuarioRepository.findByCorreoIgnoreCase(correo)).as("sin usuario huérfano").isEmpty();

        // y el correo no quedó ocupado: un alta posterior correcta funciona
        reset(perfilUsuarioService);
        registrar(correo, "Clave-Valida-1", null).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("E03/E14: teléfono de 300 caracteres → 400 de validación, sin registros parciales")
    void telefonoExcesivo() throws Exception {
        String correo = correoUnico();
        registrar(correo, "Clave-Valida-1", "9".repeat(300))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.telefono").exists());
        assertThat(usuarioRepository.findByCorreoIgnoreCase(correo)).isEmpty();
        registrar(correo, "Clave-Valida-1", "9".repeat(20)).andExpect(status().isCreated()); // frontera válida
    }

    @Test
    @DisplayName("E03: correo duplicado → 409 con mensaje, sin duplicar filas")
    void correoDuplicado() throws Exception {
        String correo = correoUnico();
        registrar(correo, "Clave-Valida-1", null).andExpect(status().isCreated());
        registrar(correo.toUpperCase(), "Otra-Clave-2", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El correo electrónico ya se encuentra registrado."));
        assertThat(usuarioRepository.findAll().stream().filter(u -> u.getCorreo().equalsIgnoreCase(correo)).count()).isEqualTo(1);
    }

    // ------------------------------------------------------------------ E04

    /** Correo válido (@Email: etiquetas <= 63) de exactamente `total` caracteres. */
    private String correoDeLongitud(int total) {
        String local = ("a".repeat(20) + UUID.randomUUID().toString().replace("-", "")).substring(0, 32);
        String dominio = "d".repeat(50) + "." + "e".repeat(50) + "." + "f".repeat(total - local.length() - 1 - 50 - 1 - 50 - 1);
        return local + "@" + dominio;
    }

    @Test
    @DisplayName("E04: correo de 150 caracteres se acepta y permite volver a iniciar sesión; 151 se rechaza")
    void limiteDeCorreo() throws Exception {
        String correo150 = correoDeLongitud(150);
        assertThat(correo150).hasSize(150);
        registrar(correo150, "Clave-Valida-1", null).andExpect(status().isCreated());
        login(correo150, "Clave-Valida-1").andExpect(status().isOk()).andExpect(jsonPath("$.token").isString());

        String correo151 = correo150.replaceFirst("^a", "aa");
        assertThat(correo151).hasSize(151);
        registrar(correo151, "Clave-Valida-1", null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.correo").exists());
        login(correo151, "Clave-Valida-1").andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ E05

    @Test
    @DisplayName("E05: una contraseña que empieza por $2a$/$2b$ se trata como texto y permite iniciar sesión")
    void passwordConPrefijoBcrypt() throws Exception {
        for (String prefijo : new String[]{"$2a$10$", "$2b$12$"}) {
            String password = prefijo + "abcdefghijklmnopqrstuv"; // 29 caracteres, "parece" un hash
            String correo = correoUnico();
            registrar(correo, password, null).andExpect(status().isCreated());
            login(correo, password).andExpect(status().isOk());
            var guardada = usuarioRepository.findByCorreoIgnoreCase(correo).orElseThrow().getPassword();
            assertThat(guardada).isNotEqualTo(password).startsWith("$2a$"); // se codificó, no se guardó tal cual
        }
    }

    @Test
    @DisplayName("E05: límite BCrypt en bytes UTF-8 (72 ok, 74 rechazado aunque tenga 37 caracteres)")
    void limiteBcryptEnBytes() throws Exception {
        String ok = "é".repeat(36);        // 36 caracteres = 72 bytes
        String excede = "é".repeat(37);    // 37 caracteres = 74 bytes
        String correoOk = correoUnico();
        registrar(correoOk, ok, null).andExpect(status().isCreated());
        login(correoOk, ok).andExpect(status().isOk());

        registrar(correoUnico(), excede, null).andExpect(status().isBadRequest());
        assertThat(usuarioRepository.findByCorreoIgnoreCase(correos.get(correos.size() - 1))).isEmpty();
        registrar(correoUnico(), "x".repeat(73), null).andExpect(status().isBadRequest());
    }
}
