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
    private cl.nicolet.backend.service.AuthService authService;

    @Autowired
    private cl.nicolet.backend.security.ClientIpResolver clientIpResolver;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener token JWT")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest, HttpServletRequest request) {
        if (loginRequest.getPassword() != null && loginRequest.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", 400, "codigo", "VALIDACION",
                    "mensaje", "La contraseña no puede exceder los 72 caracteres",
                    "errores", Map.of("password", "La contraseña no puede exceder los 72 caracteres"),
                    "password", "La contraseña no puede exceder los 72 caracteres"
            ));
        }
        String correo = loginRequest.getCorreo() != null ? loginRequest.getCorreo().trim().toLowerCase() : "";
        String clientIp = obtenerIpCliente(request);

        String accountKey = "account:" + correo;
        String ipKey = "ip:" + clientIp;
        // Contamos el intento antes de revisar la contraseña para evitar muchos intentos al mismo tiempo.
        // Si el login sale bien o hay un error del servidor, se devuelve el intento.
        if (!loginAttemptService.tryAcquire(accountKey, ipKey)) {
            log.warn("Bloqueo temporal de autenticación por intentos fallidos");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                    "error", "Too Many Requests",
                    "mensaje", "Demasiados intentos fallidos. Su cuenta o dirección IP ha sido bloqueada temporalmente por 15 minutos."
            ));
        }

        log.debug("Intento de inicio de sesión recibido");

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
                            && java.security.MessageDigest.isEqual(
                                    u.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                    loginRequest.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                        log.info("Migrando contraseña legacy en texto plano a BCrypt durante login para id={}", u.getId());
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
            if (userDetails.getAuthorities().isEmpty()) {
                // Si la cuenta no tiene rol, no se entrega token porque no tiene permisos.
                loginAttemptService.loginSucceeded(accountKey);
                loginAttemptService.release(ipKey);
                SecurityContextHolder.clearContext();
                log.warn("Inicio de sesión rechazado: cuenta id={} sin rol asignado", userDetails.getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "status", HttpStatus.FORBIDDEN.value(),
                        "error", "Forbidden",
                        "mensaje", "Tu cuenta no tiene un rol asignado. Contacta al administrador."
                ));
            }
            String jwt = jwtUtils.generarToken(userDetails);

            loginAttemptService.loginSucceeded(accountKey);
            loginAttemptService.release(ipKey); // entrar bien no borra los fallos anteriores de esa IP

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
            // El intento fallido ya se descontó antes.
            int intentosRestantes = loginAttemptService.getRemainingAttempts(accountKey);

            log.warn("Credenciales incorrectas. Intentos restantes para la cuenta: {}", intentosRestantes);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED.value(),
                    "error", "Unauthorized",
                    "mensaje", "Credenciales incorrectas. Verifique su correo y contraseña.",
                    "intentosRestantes", intentosRestantes
            ));
        } catch (Exception e) {
            loginAttemptService.release(accountKey); // si falla el servidor no cuenta como error de contraseña
            loginAttemptService.release(ipKey);
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
                    .orElse("SIN_ROL");

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
    public ResponseEntity<?> register(@Valid @RequestBody RegistroRequestDTO request, HttpServletRequest httpRequest) {
        if (request.getPassword() != null && request.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", 400, "codigo", "VALIDACION",
                    "mensaje", "La contraseña debe tener entre 8 y 72 caracteres",
                    "errores", Map.of("password", "La contraseña debe tener entre 8 y 72 caracteres"),
                    "password", "La contraseña debe tener entre 8 y 72 caracteres"
            ));
        }
        if (loginAttemptService.registroBloqueado(obtenerIpCliente(httpRequest))) {
            log.warn("Registro rechazado: demasiadas cuentas solicitadas desde la misma dirección IP");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                    "error", "Too Many Requests",
                    "mensaje", "Se crearon demasiadas cuentas desde esta conexión. Inténtalo de nuevo más tarde."
            ));
        }
        log.info("Solicitud de registro de nuevo usuario");

        // Crea el usuario y su perfil de cliente juntos; si el correo ya existe devuelve error 409.
        Usuario usuarioGuardado = authService.registrarCliente(request);
        UserDetailsImpl userDetails = UserDetailsImpl.build(usuarioGuardado);
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
        return clientIpResolver.resolve(request);
    }
}
