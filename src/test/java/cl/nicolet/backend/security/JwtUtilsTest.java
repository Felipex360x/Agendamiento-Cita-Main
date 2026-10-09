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
    @DisplayName("generarToken - debe crear un token JWT válido con el correo en el subject")
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
        assertEquals("valentina.soto@gmail.com", jwtUtils.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("validarToken - debe retornar false si el token está adulterado o malformado")
    void debeRechazarTokenInvalido() {
        assertFalse(jwtUtils.validarToken("token.invalido.falso"));
        assertFalse(jwtUtils.validarToken(""));
        assertFalse(jwtUtils.validarToken(null));
    }
}
