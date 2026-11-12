package cl.nicolet.backend.security;

import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.repository.TipoUsuarioRepository;
import cl.nicolet.backend.repository.UsuarioRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas para verificar que el token JWT identifica al usuario por su ID fijo.
 */
@SpringBootTest
@AutoConfigureMockMvc
class JwtIdentityIntegrationTest {

    private static final String ME = "/api/v2/nicolet/auth/me";
    private static final String USUARIOS = "/api/v2/nicolet/usuarios";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TipoUsuarioRepository tipoUsuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;
    @Value("${jwt.secret}") private String jwtSecret;

    private final List<Long> creados = new ArrayList<>();

    @AfterEach
    void limpiar() {
        usuarioRepository.deleteAllById(creados);
        creados.clear();
    }

    private Usuario crearUsuario(String correo, String rol) {
        Usuario u = new Usuario();
        u.setNombre("Test");
        u.setApellidoP("Identidad");
        u.setCorreo(correo);
        u.setPassword(passwordEncoder.encode("Clave-Segura-123"));
        u.setTipoUsuario(rol == null ? null : tipoUsuarioRepository.findByNombreIgnoreCase(rol).orElseThrow());
        Usuario guardado = usuarioRepository.save(u);
        creados.add(guardado.getId());
        return guardado;
    }

    private String correoUnico(String prefijo) {
        return prefijo + "." + UUID.randomUUID().toString().substring(0, 8) + "@identidad.test";
    }

    private String tokenDe(Usuario u) {
        return jwtUtils.generarToken(UserDetailsImpl.build(u));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String tokenManual(Consumer<io.jsonwebtoken.JwtBuilder> custom, long ttlMs) {
        var b = Jwts.builder()
                .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                .expiration(new Date(System.currentTimeMillis() + ttlMs));
        custom.accept(b);
        return b.signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret))).compact();
    }

    @Test
    @DisplayName("Un token válido autentica a su dueño por ID")
    void tokenValidoAutenticaPorId() throws Exception {
        Usuario a = crearUsuario(correoUnico("a"), "CLIENTE");
        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(tokenDe(a))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(a.getId()));
    }

    @Test
    @DisplayName("Reutilizar el correo anterior de A no permite que su token autentique a B")
    void cambioYReutilizacionDeCorreo() throws Exception {
        String correoX = correoUnico("x");
        Usuario a = crearUsuario(correoX, "CLIENTE");
        String tokenA = tokenDe(a);

        a.setCorreo(correoUnico("y"));
        usuarioRepository.save(a);
        Usuario b = crearUsuario(correoX, "ADMINISTRADOR"); // el usuario B usa el correo que dejó libre A

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(a.getId()))
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(b.getId().intValue())))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
        // el token de A no da permisos de administrador de B
        mockMvc.perform(get(USUARIOS).header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un usuario desactivado pierde el acceso con su token vigente")
    void usuarioDesactivado() throws Exception {
        Usuario a = crearUsuario(correoUnico("d"), "CLIENTE");
        String token = tokenDe(a);
        a.setEstado(Usuario.ESTADO_DESACTIVADO);
        usuarioRepository.save(a);

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un token con el formato anterior (subject = correo) se rechaza con 401")
    void formatoAnteriorRechazado() throws Exception {
        Usuario a = crearUsuario(correoUnico("legacy"), "ADMINISTRADOR");
        String viejo = tokenManual(b -> b.subject(a.getCorreo()).claim("id", a.getId()).claim("rol", "ROLE_ADMINISTRADOR"), 60_000);

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(viejo)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(USUARIOS).header(HttpHeaders.AUTHORIZATION, bearer(viejo)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Identidad discordante entre subject e id se rechaza")
    void identidadDiscordante() throws Exception {
        Usuario cliente = crearUsuario(correoUnico("c"), "CLIENTE");
        Usuario admin = crearUsuario(correoUnico("adm"), "ADMINISTRADOR");
        // el subject tiene el ID del admin pero el campo id tiene el del cliente
        String t = tokenManual(b -> b.subject(String.valueOf(admin.getId()))
                .claim("ver", JwtUtils.TOKEN_VERSION).claim("id", cliente.getId()), 60_000);

        mockMvc.perform(get(USUARIOS).header(HttpHeaders.AUTHORIZATION, bearer(t)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token expirado, con firma inválida o malformado → 401")
    void expiradoFirmaInvalidaYMalformado() throws Exception {
        Usuario a = crearUsuario(correoUnico("e"), "CLIENTE");
        String expirado = tokenManual(b -> b.subject(String.valueOf(a.getId()))
                .claim("ver", JwtUtils.TOKEN_VERSION).claim("id", a.getId()), -5_000);
        String otraClave = Jwts.builder().subject(String.valueOf(a.getId()))
                .claim("ver", JwtUtils.TOKEN_VERSION).claim("id", a.getId())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")))
                .compact();

        for (String t : List.of(expirado, otraClave, "no.es.un.jwt")) {
            mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(t)))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(get(ME)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Los permisos son los actuales en BD, no el rol que traía el token")
    void rolActualNoElDelToken() throws Exception {
        Usuario a = crearUsuario(correoUnico("rol"), "ADMINISTRADOR");
        String token = tokenDe(a); // el token dice que era ADMINISTRADOR

        mockMvc.perform(get(USUARIOS).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        a.setTipoUsuario(tipoUsuarioRepository.findByNombreIgnoreCase("CLIENTE").orElseThrow());
        usuarioRepository.save(a);

        mockMvc.perform(get(USUARIOS).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }
}
