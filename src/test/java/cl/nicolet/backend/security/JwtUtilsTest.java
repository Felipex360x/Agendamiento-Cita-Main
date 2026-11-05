package cl.nicolet.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3600000L); // 1 hora
    }

    @Test
    @DisplayName("generarToken - debe crear un token JWT válido con el ID permanente en el subject")
    void debeGenerarTokenValido() {
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L,
                "Valentina",
                "Soto",
                "valentina.soto@gmail.com",
                "pass_encoded",
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))
        );

        String token = jwtUtils.generarToken(userDetails);

        assertNotNull(token);
        assertTrue(token.length() > 20);
        assertTrue(jwtUtils.validarToken(token));
        assertEquals(java.util.Optional.of(1L), jwtUtils.getUserIdFromToken(token));
        assertEquals("1", jwtUtils.getClaimsFromToken(token).getSubject());
    }

    @Test
    @DisplayName("validarToken - debe retornar false si el token está adulterado o malformado")
    void debeRechazarTokenInvalido() {
        assertFalse(jwtUtils.validarToken("token.invalido.falso"));
        assertFalse(jwtUtils.validarToken(""));
        assertFalse(jwtUtils.validarToken(null));
    }

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private String tokenManual(java.util.function.Consumer<io.jsonwebtoken.JwtBuilder> custom, String secret, long ttlMs) {
        var b = io.jsonwebtoken.Jwts.builder()
                .issuedAt(new java.util.Date(System.currentTimeMillis() - 10_000))
                .expiration(new java.util.Date(System.currentTimeMillis() + ttlMs));
        custom.accept(b);
        return b.signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                io.jsonwebtoken.io.Decoders.BASE64.decode(secret))).compact();
    }

    @Test
    @DisplayName("getUserIdFromToken - rechaza el formato anterior (subject = correo, sin versión)")
    void rechazaFormatoAnterior() {
        String viejo = tokenManual(b -> b.subject("a@x.cl").claim("id", 1L).claim("rol", "ROLE_ADMINISTRADOR"), SECRET, 60_000);
        assertTrue(jwtUtils.validarToken(viejo), "la firma es válida, pero el formato no debe aceptarse");
        assertTrue(jwtUtils.getUserIdFromToken(viejo).isEmpty());
    }

    @Test
    @DisplayName("getUserIdFromToken - rechaza identidad discordante entre subject e id")
    void rechazaIdentidadDiscordante() {
        String t = tokenManual(b -> b.subject("1").claim("ver", JwtUtils.TOKEN_VERSION).claim("id", 2L), SECRET, 60_000);
        assertTrue(jwtUtils.getUserIdFromToken(t).isEmpty());
        String sinId = tokenManual(b -> b.subject("1").claim("ver", JwtUtils.TOKEN_VERSION), SECRET, 60_000);
        assertTrue(jwtUtils.getUserIdFromToken(sinId).isEmpty());
    }

    @Test
    @DisplayName("getUserIdFromToken - rechaza token expirado y firma de otra clave")
    void rechazaExpiradoYFirmaInvalida() {
        String expirado = tokenManual(b -> b.subject("1").claim("ver", JwtUtils.TOKEN_VERSION).claim("id", 1L), SECRET, -5_000);
        assertTrue(jwtUtils.getUserIdFromToken(expirado).isEmpty());
        String otraClave = tokenManual(b -> b.subject("1").claim("ver", JwtUtils.TOKEN_VERSION).claim("id", 1L),
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 60_000);
        assertTrue(jwtUtils.getUserIdFromToken(otraClave).isEmpty());
        assertTrue(jwtUtils.getUserIdFromToken("basura").isEmpty());
        assertTrue(jwtUtils.getUserIdFromToken(null).isEmpty());
    }
}
