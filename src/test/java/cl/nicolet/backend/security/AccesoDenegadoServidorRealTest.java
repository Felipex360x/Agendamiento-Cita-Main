package cl.nicolet.backend.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MockMvc no pasa por el contenedor de servlets, por eso no detecta que un 403 se convierta en 401 al
 * redirigirse a /error. Esta prueba levanta un servidor real y comprueba el código HTTP que ve el cliente.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccesoDenegadoServidorRealTest {

    private static final String API = "/api/v2/nicolet";
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @LocalServerPort private int puerto;
    @Autowired private LoginAttemptService loginAttemptService;

    private HttpResponse<String> enviar(String metodo, String ruta, String token, String cuerpo) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + puerto + API + ruta))
                .header("Content-Type", "application/json")
                .method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(cuerpo));
        if (token != null) b.header("Authorization", "Bearer " + token);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String tokenDeClienteNuevo() throws Exception {
        loginAttemptService.reset();
        String correo = "cliente." + UUID.randomUUID().toString().substring(0, 8) + "@acceso.test";
        String clave = "ClaveSegura123!";
        HttpResponse<String> reg = enviar("POST", "/auth/register", null,
                "{\"nombre\":\"Cli\",\"apellidoP\":\"Ente\",\"correo\":\"" + correo + "\",\"password\":\"" + clave + "\"}");
        assertThat(reg.statusCode()).isEqualTo(201);
        HttpResponse<String> login = enviar("POST", "/auth/login", null,
                "{\"correo\":\"" + correo + "\",\"password\":\"" + clave + "\"}");
        assertThat(login.statusCode()).isEqualTo(200);
        return json.readTree(login.body()).get("token").asText();
    }

    @Test
    @DisplayName("con sesión válida pero sin rol de administración, el servidor real responde 403 (no 401)")
    void clienteEnRutaDeAdministracionRecibe403() throws Exception {
        String token = tokenDeClienteNuevo();

        for (String ruta : new String[]{"/correos", "/citas", "/usuarios"}) {
            HttpResponse<String> r = enviar("GET", ruta, token, null);
            assertThat(r.statusCode()).as("GET " + ruta).isEqualTo(403);
            JsonNode cuerpo = json.readTree(r.body());
            assertThat(cuerpo.get("status").asInt()).isEqualTo(403);
            assertThat(cuerpo.get("mensaje").asText()).isNotBlank();
        }
    }

    @Test
    @DisplayName("sin sesión sigue respondiendo 401")
    void anonimoRecibe401() throws Exception {
        assertThat(enviar("GET", "/correos", null, null).statusCode()).isEqualTo(401);
        assertThat(enviar("GET", "/correos", "token-invalido", null).statusCode()).isEqualTo(401);
    }
}
