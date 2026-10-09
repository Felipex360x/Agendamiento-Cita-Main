package cl.nicolet.backend.security;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para mitigar ataques de fuerza bruta y denegación de servicio (DoS)
 * en el inicio de sesión mediante bloqueo temporal tras múltiples intentos fallidos.
 * Incluye cuentas de prueba administrativas exentas de bloqueo para desarrollo y QA.
 */
@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    public static final long BLOCK_DURATION_MS = 15 * 60 * 1000; // 15 minutos de bloqueo

    /**
     * Cuentas de administrador de prueba siempre garantizadas y exentas de bloqueo.
     */
    public static final Set<String> TEST_ADMIN_EMAILS = Set.of(
            "admin.test@nicolet.cl",
            "admin@nicolet.cl"
    );

    public static boolean isTestAdmin(String email) {
        if (email == null) return false;
        return TEST_ADMIN_EMAILS.contains(email.toLowerCase().trim());
    }

    private static class AttemptInfo {
        final int attempts;
        final long lastAttemptTime;

        AttemptInfo(int attempts, long lastAttemptTime) {
            this.attempts = attempts;
            this.lastAttemptTime = lastAttemptTime;
        }
    }

    private final ConcurrentHashMap<String, AttemptInfo> attemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        if (key != null && !key.isBlank()) {
            attemptsCache.remove(key.toLowerCase().trim());
        }
    }

    public void loginFailed(String key) {
        if (key == null || key.isBlank()) return;
        String normalizedKey = key.toLowerCase().trim();

        // Las cuentas de test admin no acumulan fallos
        if (isTestAdmin(normalizedKey)) {
            return;
        }

        long now = System.currentTimeMillis();
        attemptsCache.compute(normalizedKey, (k, info) -> {
            if (info == null || (now - info.lastAttemptTime) > BLOCK_DURATION_MS) {
                return new AttemptInfo(1, now);
            } else {
                return new AttemptInfo(info.attempts + 1, now);
            }
        });
    }

    public boolean isBlocked(String key) {
        if (key == null || key.isBlank()) return false;
        String normalizedKey = key.toLowerCase().trim();

        // El administrador de prueba nunca queda bloqueado
        if (isTestAdmin(normalizedKey)) {
            return false;
        }

        AttemptInfo info = attemptsCache.get(normalizedKey);
        if (info == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        if ((now - info.lastAttemptTime) > BLOCK_DURATION_MS) {
            attemptsCache.remove(normalizedKey);
            return false;
        }

        return info.attempts >= MAX_ATTEMPTS;
    }

    public int getRemainingAttempts(String key) {
        if (key == null || key.isBlank()) return MAX_ATTEMPTS;
        String normalizedKey = key.toLowerCase().trim();

        if (isTestAdmin(normalizedKey)) {
            return MAX_ATTEMPTS;
        }

        AttemptInfo info = attemptsCache.get(normalizedKey);
        if (info == null) return MAX_ATTEMPTS;

        long now = System.currentTimeMillis();
        if ((now - info.lastAttemptTime) > BLOCK_DURATION_MS) {
            attemptsCache.remove(normalizedKey);
            return MAX_ATTEMPTS;
        }

        return Math.max(0, MAX_ATTEMPTS - info.attempts);
    }

    public void reset() {
        attemptsCache.clear();
    }
}
