package cl.nicolet.backend.security;

import cl.nicolet.backend.dto.LoginRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class LoginSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService.reset();
    }

    @Test
    @DisplayName("Login exitoso devuelve JWT y oculta la contraseña")
    void testLoginExitoso() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("maximo.rojas@estudio.cl", "password_seguro_123");

        MvcResult result = mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tipoToken").value("Bearer"))
                .andExpect(jsonPath("$.correo").value("maximo.rojas@estudio.cl"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain("password_seguro_123");
        assertThat(responseBody).doesNotContain("$2a$");
    }

    @Test
    @DisplayName("Contraseña incorrecta devuelve 401")
    void testLoginPasswordIncorrecta() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("maximo.rojas@estudio.cl", "PasswordIncorrecta999!");

        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas. Verifique su correo y contraseña."))
                .andExpect(jsonPath("$.intentosRestantes").value(4));
    }

    @Test
    @DisplayName("Usuario inexistente devuelve 401")
    void testAntiEnumeracionUsuarioInexistente() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("usuario_fantasma_no_existe@estudio.cl", "CualquierClave123!");

        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas. Verifique su correo y contraseña."));
    }

    @Test
    @DisplayName("Rechazar correo vacío con 400")
    void testRechazoCorreoVacio() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("", "Password123!");

        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.correo").exists());
    }

    @Test
    @DisplayName("Rechazar correo con formato inválido con 400")
    void testRechazoInyeccionSQLYCorreoMalformado() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("' OR 1=1 --", "Password123!");

        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.correo").exists());
    }

    @Test
    @DisplayName("Rechazar contraseña vacía con 400")
    void testRechazoPasswordVacia() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("maximo.rojas@estudio.cl", "   ");

        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    @DisplayName("Rechazar contraseña mayor a 72 caracteres")
    void testRechazoPasswordDemasiadoLarga_AntiDoS() throws Exception {
        String passwordGigante = "A".repeat(73);
        LoginRequestDTO request = new LoginRequestDTO("maximo.rojas@estudio.cl", passwordGigante);

        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value("La contraseña no puede exceder los 72 caracteres"));
    }

    @Test
    @DisplayName("Limpiar espacios en blanco del correo al iniciar sesión")
    void testSanitizacionEspaciosEnBlancoCorreo() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("  maximo.rojas@estudio.cl  ", "password_seguro_123");

        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.correo").value("maximo.rojas@estudio.cl"));
    }

    @Test
    @DisplayName("Bloquear temporalmente tras 5 intentos fallidos")
    void testProteccionFuerzaBrutaRateLimiting() throws Exception {
        LoginRequestDTO intentoInvalido = new LoginRequestDTO("maximo.rojas@estudio.cl", "clave_mala");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v2/nicolet/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(intentoInvalido)))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(intentoInvalido)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.mensaje").value("Demasiados intentos fallidos. Su cuenta o dirección IP ha sido bloqueada temporalmente por 15 minutos."));
    }

    @Test
    @DisplayName("Endpoint protegido rechaza peticiones sin token con 401")
    void testEndpointProtegidoSinToken() throws Exception {
        mockMvc.perform(get("/api/v2/nicolet/citas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").value("Acceso denegado: debe autenticarse para acceder a este recurso."));
    }

    @Test
    @DisplayName("Endpoint protegido rechaza token inválido con 401")
    void testEndpointProtegidoTokenFalso() throws Exception {
        mockMvc.perform(get("/api/v2/nicolet/citas")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.token_falso_manipulado.firma_invalida"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Endpoint protegido rechaza header Authorization sin Bearer")
    void testEndpointProtegidoHeaderSinBearer() throws Exception {
        mockMvc.perform(get("/api/v2/nicolet/citas")
                        .header("Authorization", "Basic dXN1YXJpbzpjb250cmFzZW5h"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Login exitoso permite consultar /me con el token generado")
    void testFlujoCompletoLoginYConsumoMe() throws Exception {
        LoginRequestDTO login = new LoginRequestDTO("maximo.rojas@estudio.cl", "password_seguro_123");
        MvcResult loginResult = mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/v2/nicolet/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("maximo.rojas@estudio.cl"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("Usuario admin de prueba puede iniciar sesión")
    void testAdminTestAccesoGarantizado() throws Exception {
        LoginRequestDTO login = new LoginRequestDTO("admin.test@nicolet.cl", "AdminTest123!");

        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.correo").value("admin.test@nicolet.cl"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("El administrador también queda bloqueado tras intentos fallidos")
    void testAdministradorNoOmiteBloqueoPorFuerzaBruta() throws Exception {
        LoginRequestDTO ataque = new LoginRequestDTO("admin.test@nicolet.cl", "clave_falsa");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v2/nicolet/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ataque)))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ataque)))
                .andExpect(status().isTooManyRequests());

        LoginRequestDTO adminTest = new LoginRequestDTO("admin.test@nicolet.cl", "AdminTest123!");
        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminTest)))
                .andExpect(status().isTooManyRequests());

        loginAttemptService.reset();
        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminTest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("admin.test@nicolet.cl"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }
}
