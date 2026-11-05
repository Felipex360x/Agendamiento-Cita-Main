package cl.nicolet.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * O07: con Vite (127.0.0.1) como proxy de confianza, el límite por IP se aplica por cliente real y no
 * bloquea a todos los usuarios que comparten el proxy.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.security.trusted-proxies=127.0.0.1")
class RateLimitProxyIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private LoginAttemptService loginAttemptService;

    @BeforeEach
    void limpiar() { loginAttemptService.reset(); }

    private int intento(String ipCliente, String forwardedExtra) throws Exception {
        String correo = "fantasma." + UUID.randomUUID().toString().substring(0, 8) + "@limite.test"; // cuenta distinta cada vez
        String forwarded = forwardedExtra == null ? ipCliente : forwardedExtra + ", " + ipCliente;
        return mockMvc.perform(post("/api/v2/nicolet/auth/login").header("X-Forwarded-For", forwarded)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"password\":\"incorrecta-123\"}"))
                .andReturn().getResponse().getStatus();
    }

    @Test
    @DisplayName("cinco fallos de un cliente lo bloquean a él, no al resto de clientes del mismo proxy")
    void bloqueoAislado() throws Exception {
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            assertThat(intento("203.0.113.5", null)).isEqualTo(401);
        }
        assertThat(intento("203.0.113.5", null)).as("cliente A bloqueado").isEqualTo(429);
        assertThat(intento("203.0.113.6", null)).as("cliente B no afectado").isEqualTo(401);
    }

    @Test
    @DisplayName("forjar X-Forwarded-For no evita ni transfiere el bloqueo (se usa la IP que añadió el proxy)")
    void suplantacion() throws Exception {
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            intento("203.0.113.5", "9.9.9." + i); // el atacante rota la IP que declara
        }
        assertThat(intento("203.0.113.5", "1.1.1.1")).as("sigue bloqueado pese a declarar otra IP").isEqualTo(429);
        assertThat(intento("203.0.113.9", "203.0.113.5")).as("declarar la IP bloqueada no bloquea a un tercero").isEqualTo(401);
    }

    @Test
    @DisplayName("ráfaga concurrente: tras el límite, el resto de intentos del mismo cliente se rechaza")
    void rafagaConcurrente() throws Exception {
        int total = 30;
        ExecutorService pool = Executors.newFixedThreadPool(10);
        List<Future<Integer>> futuros = new ArrayList<>();
        for (int i = 0; i < total; i++) futuros.add(pool.submit(() -> intento("203.0.113.50", null)));
        long ok401 = 0, bloqueados = 0;
        for (Future<Integer> f : futuros) { int s = f.get(60, TimeUnit.SECONDS); if (s == 401) ok401++; else if (s == 429) bloqueados++; }
        pool.shutdown();
        assertThat(ok401 + bloqueados).isEqualTo(total);
        assertThat(bloqueados).as("la ráfaga acaba bloqueada").isGreaterThanOrEqualTo(total - 2L * LoginAttemptService.MAX_ATTEMPTS);
        assertThat(intento("203.0.113.51", null)).as("otro cliente sigue funcionando").isEqualTo(401);
    }
}
