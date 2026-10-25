package cl.nicolet.backend.controller;

import cl.nicolet.backend.dto.AuthResponseDTO;
import cl.nicolet.backend.dto.LoginRequestDTO;
import cl.nicolet.backend.security.JwtUtils;
import cl.nicolet.backend.security.LoginAttemptService;
import cl.nicolet.backend.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import cl.nicolet.backend.dto.RegistroRequestDTO;
import cl.nicolet.backend.model.TipoUsuario;
import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.repository.TipoUsuarioRepository;
import cl.nicolet.backend.service.PerfilUsuarioService;

import java.util.Map;

@RestController
@RequestMapping("/api/v2/nicolet/auth")
@Tag(name = "Autenticación", description = "Endpoints de inicio de sesión, registro y verificación de tokens JWT")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private cl.nicolet.backend.repository.UsuarioRepository usuarioRepository;

    @Autowired
    private TipoUsuarioRepository tipoUsuarioRepository;

    @Autowired
    private PerfilUsuarioService perfilUsuarioService;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener token JWT")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest, HttpServletRequest request) {
        if (loginRequest.getPassword() != null && loginRequest.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "password", "La contraseña no puede exceder los 72 caracteres"
            ));
        }
        String correo = loginRequest.getCorreo() != null ? loginRequest.getCorreo().trim().toLowerCase() : "";
        String clientIp = obtenerIpCliente(request);

        String accountKey = "account:" + correo;
        String ipKey = "ip:" + clientIp;
        if (loginAttemptService.isBlocked(accountKey) || loginAttemptService.isBlocked(ipKey)) {
            log.warn("Bloqueo temporal de autenticación por intentos fallidos");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                    "error", "Too Many Requests",
                    "mensaje", "Demasiados intentos fallidos. Su cuenta o dirección IP ha sido bloqueada temporalmente por 15 minutos."
            ));
        }

        log.info("Intento de inicio de sesión para correo: {}", correo);

        try {
            Authentication authentication;
            try {
                authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                correo,
                                loginRequest.getPassword()
                        )
                );
            } catch (BadCredentialsException bce) {
                var usuarioOpt = usuarioRepository.findByCorreoIgnoreCase(correo);
                if (usuarioOpt.isPresent()) {
                    var u = usuarioOpt.get();
                    if (u.getPassword() != null 
                            && !u.getPassword().startsWith("$2a$") 
                            && !u.getPassword().startsWith("$2b$")
                            && u.getPassword().equals(loginRequest.getPassword())) {
                        log.info("Migrando contraseña legacy en texto plano a BCrypt durante login para: {}", correo);
                        u.setPassword(passwordEncoder.encode(loginRequest.getPassword()));
                        usuarioRepository.save(u);
                        authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(correo, loginRequest.getPassword())
                        );
                    } else {
                        throw bce;
                    }
                } else {
                    throw bce;
                }
            }

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String jwt = jwtUtils.generarToken(userDetails);

            loginAttemptService.loginSucceeded(accountKey);
            loginAttemptService.loginSucceeded(ipKey);

            String rol = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElse("CLIENTE");

            AuthResponseDTO response = AuthResponseDTO.builder()
                    .token(jwt)
                    .tipoToken("Bearer")
                    .id(userDetails.getId())
                    .nombre(userDetails.getNombre())
                    .apellidoP(userDetails.getApellidoP())
                    .correo(userDetails.getCorreo())
                    .rol(rol)
                    .build();

            log.info("Inicio de sesión exitoso para id={}, rol={}", userDetails.getId(), rol);
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(accountKey);
            loginAttemptService.loginFailed(ipKey);
            int intentosRestantes = loginAttemptService.getRemainingAttempts(accountKey);

            log.warn("Credenciales incorrectas para correo: {}. Intentos restantes: {}", correo, intentosRestantes);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED.value(),
                    "error", "Unauthorized",
                    "mensaje", "Credenciales incorrectas. Verifique su correo y contraseña.",
                    "intentosRestantes", intentosRestantes
            ));
        } catch (Exception e) {
            log.error("Error durante el inicio de sesión: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "error", "Internal Server Error",
                    "mensaje", "Error interno al procesar la autenticación."
            ));
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener información del usuario actualmente autenticado")
    public ResponseEntity<?> obtenerUsuarioActual(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED.value(),
                    "error", "Unauthorized",
                    "mensaje", "No hay una sesión activa."
            ));
        }

        String rol = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElse("CLIENTE");

        return ResponseEntity.ok(Map.of(
                "id", userDetails.getId(),
                "nombre", userDetails.getNombre() != null ? userDetails.getNombre() : "",
                "apellidoP", userDetails.getApellidoP() != null ? userDetails.getApellidoP() : "",
                "correo", userDetails.getCorreo() != null ? userDetails.getCorreo() : "",
                "rol", rol
        ));
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar nuevo usuario/cliente y obtener sesión inicial")
    public ResponseEntity<?> register(@Valid @RequestBody RegistroRequestDTO request) {
        if (request.getPassword() != null && request.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "password", "La contraseña debe tener entre 6 y 72 caracteres"
            ));
        }
        String correo = request.getCorreo() != null ? request.getCorreo().trim().toLowerCase() : "";
        log.info("Solicitud de registro de nuevo usuario con correo: {}", correo);

        if (usuarioRepository.existsByCorreoIgnoreCase(correo) || usuarioRepository.findByCorreoIgnoreCase(correo).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", HttpStatus.BAD_REQUEST.value(),
                    "error", "Bad Request",
                    "mensaje", "El correo electrónico ya se encuentra registrado."
            ));
        }

        TipoUsuario tipoCliente = tipoUsuarioRepository.findByNombreIgnoreCase("CLIENTE")
                .orElseGet(() -> {
                    TipoUsuario nuevoTipo = new TipoUsuario();
                    nuevoTipo.setNombre("CLIENTE");
                    nuevoTipo.setDescripcion("Cliente que solicita y agenda citas");
                    return tipoUsuarioRepository.save(nuevoTipo);
                });

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(request.getNombre().trim());
        nuevoUsuario.setApellidoP(request.getApellidoP().trim());
        nuevoUsuario.setCorreo(correo);
        nuevoUsuario.setPassword(passwordEncoder.encode(request.getPassword()));
        nuevoUsuario.setTipoUsuario(tipoCliente);
        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

        String telefono = request.getTelefono() != null && !request.getTelefono().isBlank()
                ? request.getTelefono().trim()
                : null;
        perfilUsuarioService.crearPerfilCliente(usuarioGuardado, telefono);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(correo, request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwt = jwtUtils.generarToken(userDetails);

        AuthResponseDTO response = AuthResponseDTO.builder()
                .token(jwt)
                .tipoToken("Bearer")
                .id(usuarioGuardado.getId())
                .nombre(usuarioGuardado.getNombre())
                .apellidoP(usuarioGuardado.getApellidoP())
                .correo(usuarioGuardado.getCorreo())
                .rol("CLIENTE")
                .build();

        log.info("Usuario registrado exitosamente con id={}, rol=CLIENTE", usuarioGuardado.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        if (request == null) return "unknown";
        // Forwarded headers are client-controlled unless a trusted proxy is configured.
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
