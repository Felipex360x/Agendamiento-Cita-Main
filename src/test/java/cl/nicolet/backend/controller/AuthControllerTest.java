package cl.nicolet.backend.controller;

import cl.nicolet.backend.dto.LoginRequestDTO;
import cl.nicolet.backend.security.JwtUtils;
import cl.nicolet.backend.security.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "cl\\.nicolet\\.backend\\.security\\..*"))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private cl.nicolet.backend.security.LoginAttemptService loginAttemptService;

    @MockBean
    private cl.nicolet.backend.repository.UsuarioRepository usuarioRepository;

    @MockBean
    private cl.nicolet.backend.repository.TipoUsuarioRepository tipoUsuarioRepository;

    @MockBean
    private cl.nicolet.backend.service.PerfilUsuarioService perfilUsuarioService;

    @MockBean
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v2/nicolet/auth/login - debe retornar HTTP 200 y token cuando las credenciales son correctas")
    void debeIniciarSesionExitosamente() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("admin@estudio.cl", "password_seguro_123");

        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L, "Maximo", "Rojas", "admin@estudio.cl", "pass_hash",
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtUtils.generarToken(any(UserDetailsImpl.class))).thenReturn("jwt.token.simulado");

        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.simulado"))
                .andExpect(jsonPath("$.correo").value("admin@estudio.cl"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("POST /api/v2/nicolet/auth/login - debe retornar HTTP 401 cuando las credenciales son incorrectas")
    void debeRechazarCredencialesIncorrectas() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("admin@estudio.cl", "wrong_password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("POST /api/v2/nicolet/auth/register - debe registrar nuevo cliente y retornar HTTP 201 con token")
    void debeRegistrarClienteExitosamente() throws Exception {
        cl.nicolet.backend.dto.RegistroRequestDTO request = new cl.nicolet.backend.dto.RegistroRequestDTO(
                "Paula", "Vargas", "paula.vargas@estudio.cl", "clave12345", "+56912345678"
        );

        when(usuarioRepository.findByCorreoIgnoreCase("paula.vargas@estudio.cl")).thenReturn(java.util.Optional.empty());

        cl.nicolet.backend.model.TipoUsuario rolCliente = new cl.nicolet.backend.model.TipoUsuario();
        rolCliente.setId(2L);
        rolCliente.setNombre("CLIENTE");
        when(tipoUsuarioRepository.findByNombreIgnoreCase("CLIENTE")).thenReturn(java.util.Optional.of(rolCliente));

        when(passwordEncoder.encode("clave12345")).thenReturn("encoded_pass");

        cl.nicolet.backend.model.Usuario usuarioGuardado = new cl.nicolet.backend.model.Usuario();
        usuarioGuardado.setId(10L);
        usuarioGuardado.setNombre("Paula");
        usuarioGuardado.setApellidoP("Vargas");
        usuarioGuardado.setCorreo("paula.vargas@estudio.cl");
        usuarioGuardado.setTipoUsuario(rolCliente);
        when(usuarioRepository.save(any(cl.nicolet.backend.model.Usuario.class))).thenReturn(usuarioGuardado);

        UserDetailsImpl userDetails = new UserDetailsImpl(
                10L, "Paula", "Vargas", "paula.vargas@estudio.cl", "encoded_pass",
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtUtils.generarToken(any(UserDetailsImpl.class))).thenReturn("jwt.token.registro");

        mockMvc.perform(post("/api/v2/nicolet/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt.token.registro"))
                .andExpect(jsonPath("$.correo").value("paula.vargas@estudio.cl"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    @DisplayName("POST /api/v2/nicolet/auth/register - debe rechazar si el correo ya está registrado con HTTP 400")
    void debeRechazarRegistroSiCorreoYaExiste() throws Exception {
        cl.nicolet.backend.dto.RegistroRequestDTO request = new cl.nicolet.backend.dto.RegistroRequestDTO(
                "Paula", "Vargas", "paula.vargas@estudio.cl", "clave12345", "+56912345678"
        );

        when(usuarioRepository.findByCorreoIgnoreCase("paula.vargas@estudio.cl"))
                .thenReturn(java.util.Optional.of(new cl.nicolet.backend.model.Usuario()));

        mockMvc.perform(post("/api/v2/nicolet/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El correo electrónico ya se encuentra registrado."));
    }
}
