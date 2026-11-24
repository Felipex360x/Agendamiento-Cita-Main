package cl.nicolet.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Responde 403 directamente cuando el usuario tiene sesión pero no el permiso necesario. Con el manejador por
 * defecto el 403 se redirige a /error, esa ruta se evalúa sin sesión y el cliente terminaba viendo un 401
 * (y el frontend lo tomaba como sesión vencida).
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(JwtAccessDeniedHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.warn("Acceso prohibido en {}: {}", request.getRequestURI(), accessDeniedException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpServletResponse.SC_FORBIDDEN);
        body.put("error", "Forbidden");
        body.put("mensaje", "No tiene permisos para realizar esta acción.");
        body.put("path", request.getRequestURI());
        body.put("timestamp", LocalDateTime.now().toString());
        MAPPER.writeValue(response.getOutputStream(), body);
    }
}
