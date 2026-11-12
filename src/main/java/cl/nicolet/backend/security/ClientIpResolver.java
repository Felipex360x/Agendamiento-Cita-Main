package cl.nicolet.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Obtiene la IP real del cliente para controlar el límite de intentos de login.
 * Solo usa la cabecera X-Forwarded-For si la petición viene de un proxy seguro configurado.
 * Revisa la lista de IPs de derecha a izquierda saltando los proxies seguros hasta encontrar la IP del cliente.
 */
@Component
public class ClientIpResolver {

    private static final Pattern IP_LITERAL = Pattern.compile("^[0-9a-fA-F:.]+$");

    private final List<IpAddressMatcher> trustedProxies = new ArrayList<>();

    public ClientIpResolver(@Value("${app.security.trusted-proxies:}") String trustedProxiesCsv) {
        if (trustedProxiesCsv != null) {
            for (String entry : trustedProxiesCsv.split(",")) {
                String trimmed = entry.trim();
                if (!trimmed.isEmpty()) {
                    trustedProxies.add(new IpAddressMatcher(trimmed));
                }
            }
        }
    }

    public String resolve(HttpServletRequest request) {
        if (request == null) return "unknown";
        String remote = request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
        if (trustedProxies.isEmpty() || !isTrusted(remote)) {
            return remote; // si no viene de un proxy seguro, usamos la IP directa
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) return remote;

        String[] chain = forwarded.split(",");
        for (int i = chain.length - 1; i >= 0; i--) {
            String candidate = chain[i].trim();
            if (!IP_LITERAL.matcher(candidate).matches()) {
                return remote; // si hay un texto inválido en la cabecera, usamos la IP directa
            }
            if (!isTrusted(candidate)) {
                return candidate;
            }
        }
        return remote;
    }

    private boolean isTrusted(String address) {
        for (IpAddressMatcher matcher : trustedProxies) {
            try {
                if (matcher.matches(address)) return true;
            } catch (IllegalArgumentException ignored) {
                // formato de IP no válido
            }
        }
        return false;
    }
}
