package cl.nicolet.backend.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** O07: la IP del cliente solo se toma de X-Forwarded-For cuando viene de un proxy de confianza. */
class ClientIpResolverTest {

    private MockHttpServletRequest request(String remote, String forwarded) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr(remote);
        if (forwarded != null) req.addHeader("X-Forwarded-For", forwarded);
        return req;
    }

    @Test
    @DisplayName("sin proxies de confianza se ignora X-Forwarded-For")
    void sinProxiesDeConfianza() {
        ClientIpResolver resolver = new ClientIpResolver("");
        assertEquals("127.0.0.1", resolver.resolve(request("127.0.0.1", "203.0.113.5")));
    }

    @Test
    @DisplayName("proxy de confianza (Vite en loopback): se usa la IP añadida por el proxy")
    void proxyDeConfianza() {
        ClientIpResolver resolver = new ClientIpResolver("127.0.0.1,::1");
        assertEquals("203.0.113.5", resolver.resolve(request("127.0.0.1", "203.0.113.5")));
        // Cloudflare -> cloudflared (loopback) -> Vite -> Spring: "cliente, loopback"
        assertEquals("203.0.113.5", resolver.resolve(request("127.0.0.1", "203.0.113.5, 127.0.0.1")));
        assertEquals("203.0.113.5", resolver.resolve(request("::1", "203.0.113.5")));
    }

    @Test
    @DisplayName("un cliente no puede suplantar la IP: se ignora lo que escribió a la izquierda de la cadena")
    void suplantacionIgnorada() {
        ClientIpResolver resolver = new ClientIpResolver("127.0.0.1");
        // el atacante envió "9.9.9.9"; el proxy añadió su IP real al final
        assertEquals("203.0.113.5", resolver.resolve(request("127.0.0.1", "9.9.9.9, 203.0.113.5")));
        assertEquals("203.0.113.5", resolver.resolve(request("127.0.0.1", "9.9.9.9, 8.8.8.8, 203.0.113.5, 127.0.0.1")));
    }

    @Test
    @DisplayName("una conexión directa que NO es de confianza no puede inyectar X-Forwarded-For")
    void conexionDirectaNoConfiable() {
        ClientIpResolver resolver = new ClientIpResolver("127.0.0.1");
        assertEquals("198.51.100.77", resolver.resolve(request("198.51.100.77", "1.2.3.4")));
    }

    @Test
    @DisplayName("valores inválidos, vacíos o CIDR: comportamiento seguro")
    void casosBorde() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.0/8, 127.0.0.1");
        assertEquals("127.0.0.1", resolver.resolve(request("127.0.0.1", "no-es-una-ip")));
        assertEquals("127.0.0.1", resolver.resolve(request("127.0.0.1", "")));
        assertEquals("127.0.0.1", resolver.resolve(request("127.0.0.1", null)));
        assertEquals("203.0.113.5", resolver.resolve(request("10.1.2.3", "203.0.113.5, 10.9.9.9")));
        assertEquals("unknown", resolver.resolve(null));
    }
}
