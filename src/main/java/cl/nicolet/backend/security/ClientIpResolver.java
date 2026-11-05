package cl.nicolet.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Determina la IP real del cliente para el límite de intentos de login.
 * <p>
 * {@code X-Forwarded-For} solo se considera si la conexión directa proviene de un proxy de confianza
 * ({@code app.security.trusted-proxies}: IPs o CIDR separados por coma; vacío = no confiar en ninguno).
 * La cabecera se recorre de derecha a izquierda omitiendo proxies de confianza: la primera dirección que NO
 * es de confianza es el cliente. Un cliente solo controla la parte izquierda de la cabecera, así que no
 * puede suplantar la IP que añadió el último proxy de confianza.
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
            return remote; // sin proxy de confianza: las cabeceras Forwarded las controla el cliente
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) return remote;

        String[] chain = forwarded.split(",");
        for (int i = chain.length - 1; i >= 0; i--) {
            String candidate = chain[i].trim();
            if (!IP_LITERAL.matcher(candidate).matches()) {
                return remote; // valor no válido en la cadena: no se puede confiar en ella
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
                // dirección no interpretable: no es de confianza
            }
        }
        return false;
    }
}
