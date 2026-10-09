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
    @DisplayName("Seguridad Login: Autenticación exitosa devuelve JWT válido y no expone contraseña")
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
    @DisplayName("Seguridad Login: Contraseña incorrecta devuelve 401 sin fugar información")
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
    @DisplayName("Seguridad Login: Usuario inexistente devuelve mismo 401 que contraseña errónea (Anti-Enumeración)")
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
    @DisplayName("Seguridad Login: Rechazo de correo vacío o nulo con 400 Bad Request")
    void testRechazoCorreoVacio() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("", "Password123!");

        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.correo").exists());
    }

    @Test
    @DisplayName("Seguridad Login: Rechazo de correo malformado / Inyección SQL básica con 400 Bad Request")
    void testRechazoInyeccionSQLYCorreoMalformado() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("' OR 1=1 --", "Password123!");

        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.correo").exists());
    }

    @Test
    @DisplayName("Seguridad Login: Rechazo de contraseña vacía con 400 Bad Request")
    void testRechazoPasswordVacia() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("maximo.rojas@estudio.cl", "   ");

        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    @DisplayName("Seguridad Login: Rechazo de contraseña mayor a 72 caracteres (Anti-DoS y límite BCrypt)")
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
    @DisplayName("Seguridad Login: Normalización y saneamiento de espacios en blanco en correo")
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
    @DisplayName("Seguridad Login: Bloqueo anti-fuerza bruta tras 5 intentos fallidos retorna HTTP 429 Too Many Requests")
    void testProteccionFuerzaBrutaRateLimiting() throws Exception {
        LoginRequestDTO intentoInvalido = new LoginRequestDTO("maximo.rojas@estudio.cl", "clave_mala");

        // 5 intentos fallidos permitidos
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v2/nicolet/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(intentoInvalido)))
                    .andExpect(status().isUnauthorized());
        }

        // El 6to intento consecutivo debe ser rechazado inmediatamente con HTTP 429 sin consultar la BD
        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(intentoInvalido)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.mensaje").value("Demasiados intentos fallidos. Su cuenta o dirección IP ha sido bloqueada temporalmente por 15 minutos."));
    }

    @Test
    @DisplayName("Seguridad API: Endpoint protegido rechaza peticiones sin token con 401 Unauthorized")
    void testEndpointProtegidoSinToken() throws Exception {
        mockMvc.perform(get("/api/v2/nicolet/citas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").value("Acceso denegado: debe autenticarse para acceder a este recurso."));
    }

    @Test
    @DisplayName("Seguridad API: Endpoint protegido rechaza token falso o manipulado con 401 Unauthorized")
    void testEndpointProtegidoTokenFalso() throws Exception {
        mockMvc.perform(get("/api/v2/nicolet/citas")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.token_falso_manipulado.firma_invalida"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Seguridad API: Endpoint protegido rechaza header Authorization sin prefijo Bearer")
    void testEndpointProtegidoHeaderSinBearer() throws Exception {
        mockMvc.perform(get("/api/v2/nicolet/citas")
                        .header("Authorization", "Basic dXN1YXJpbzpjb250cmFzZW5h"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Seguridad Flujo Completo: Login exitoso genera token que permite consumir /me")
    void testFlujoCompletoLoginYConsumoMe() throws Exception {
        // 1. Iniciar sesión
        LoginRequestDTO login = new LoginRequestDTO("maximo.rojas@estudio.cl", "password_seguro_123");
        MvcResult loginResult = mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
        assertThat(token).isNotBlank();

        // 2. Consumir /me con el Bearer token
        mockMvc.perform(get("/api/v2/nicolet/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("maximo.rojas@estudio.cl"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("Admin Test: Usuario administrador de prueba garantizado siempre permite ingresar")
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
    @DisplayName("Admin Test: Acceso garantizado incluso si la IP está bloqueada por intentos fallidos previos")
    void testAdminTestSuperaBloqueoPorFuerzaBruta() throws Exception {
        LoginRequestDTO ataque = new LoginRequestDTO("maximo.rojas@estudio.cl", "clave_falsa");

        // Bloquear la IP con 5 intentos fallidos
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v2/nicolet/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ataque)))
                    .andExpect(status().isUnauthorized());
        }

        // El usuario normal está bloqueado (429)
        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ataque)))
                .andExpect(status().isTooManyRequests());

        // El Admin Test siempre puede ingresar sin importar el bloqueo de IP y lo desbloquea
        LoginRequestDTO adminTest = new LoginRequestDTO("admin.test@nicolet.cl", "AdminTest123!");
        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminTest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("admin.test@nicolet.cl"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }
}
