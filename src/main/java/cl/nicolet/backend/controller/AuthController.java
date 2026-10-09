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

import java.util.Map;

@RestController
@RequestMapping("/api/v2/nicolet/auth")
@Tag(name = "Autenticación", description = "Endpoints de inicio de sesión y verificación de tokens JWT")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener token JWT")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest, HttpServletRequest request) {
        String correo = loginRequest.getCorreo() != null ? loginRequest.getCorreo().trim() : "";
        String clientIp = obtenerIpCliente(request);

        // 1. Verificación de tasa de intentos (Rate Limiting anti-fuerza bruta)
        boolean esTestAdmin = LoginAttemptService.isTestAdmin(correo);
        if (!esTestAdmin && (loginAttemptService.isBlocked(correo) || loginAttemptService.isBlocked(clientIp))) {
            log.warn("Bloqueo de seguridad: Demasiados intentos fallidos para correo={} o ip={}", correo, clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                    "error", "Too Many Requests",
                    "mensaje", "Demasiados intentos fallidos. Su cuenta o dirección IP ha sido bloqueada temporalmente por 15 minutos."
            ));
        }

        log.info("Intento de inicio de sesión para correo: {}", correo);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            correo,
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String jwt = jwtUtils.generarToken(userDetails);

            // Restablecer contador de intentos fallidos tras login exitoso
            loginAttemptService.loginSucceeded(correo);
            loginAttemptService.loginSucceeded(clientIp);

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
            // Registrar intento fallido
            loginAttemptService.loginFailed(correo);
            loginAttemptService.loginFailed(clientIp);
            int intentosRestantes = loginAttemptService.getRemainingAttempts(correo);

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
                "nombre", userDetails.getNombre(),
                "apellidoP", userDetails.getApellidoP(),
                "correo", userDetails.getCorreo(),
                "rol", rol
        ));
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        if (request == null) return "unknown";
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
