package cl.nicolet.backend.security;

import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    public static final long BLOCK_DURATION_MS = 15 * 60 * 1000;
    private static final int MAX_TRACKED_KEYS = 10_000;

    private static final class AttemptInfo {
        private int attempts;
        private long lastAttemptTime;

        private AttemptInfo(int attempts, long lastAttemptTime) {
            this.attempts = attempts;
            this.lastAttemptTime = lastAttemptTime;
        }
    }

    // LRU cap prevents arbitrary emails/IPs from growing this process-local store without limit.
    private final LinkedHashMap<String, AttemptInfo> attemptsCache = new LinkedHashMap<>(256, 0.75f, true);

    public synchronized void loginSucceeded(String key) {
        String normalizedKey = normalize(key);
        if (normalizedKey != null) {
            attemptsCache.remove(normalizedKey);
        }
    }

    public synchronized void loginFailed(String key) {
        String normalizedKey = normalize(key);
        if (normalizedKey == null) return;

        long now = System.currentTimeMillis();
        AttemptInfo info = attemptsCache.get(normalizedKey);
        if (info == null || isExpired(info, now)) {
            attemptsCache.put(normalizedKey, new AttemptInfo(1, now));
        } else {
            info.attempts++;
            info.lastAttemptTime = now;
        }

        while (attemptsCache.size() > MAX_TRACKED_KEYS) {
            Iterator<Map.Entry<String, AttemptInfo>> oldest = attemptsCache.entrySet().iterator();
            if (!oldest.hasNext()) break;
            oldest.next();
            oldest.remove();
        }
    }

    /**
     * Reserva atómicamente un intento para todas las claves: si alguna ya está bloqueada no consume ninguno;
     * si no, cuenta un intento en cada una ANTES de verificar la contraseña. Así una ráfaga concurrente nunca
     * puede iniciar más de MAX_ATTEMPTS verificaciones (a diferencia de «comprobar y luego incrementar»).
     * Un login correcto debe llamar a {@link #loginSucceeded} (cuenta) y {@link #release} (IP).
     */
    public synchronized boolean tryAcquire(String... keys) {
        for (String key : keys) {
            if (isBlocked(key)) return false;
        }
        for (String key : keys) {
            loginFailed(key); // registra el intento reservado
        }
        return true;
    }

    /** Devuelve un intento reservado (login correcto o error interno que no es un fallo de credenciales). */
    public synchronized void release(String key) {
        String normalizedKey = normalize(key);
        if (normalizedKey == null) return;
        AttemptInfo info = attemptsCache.get(normalizedKey);
        if (info == null) return;
        if (info.attempts <= 1) {
            attemptsCache.remove(normalizedKey);
        } else {
            info.attempts--;
        }
    }

    public synchronized boolean isBlocked(String key) {
        String normalizedKey = normalize(key);
        if (normalizedKey == null) return false;

        AttemptInfo info = attemptsCache.get(normalizedKey);
        if (info == null) return false;
        if (isExpired(info, System.currentTimeMillis())) {
            attemptsCache.remove(normalizedKey);
            return false;
        }
        return info.attempts >= MAX_ATTEMPTS;
    }

    public synchronized int getRemainingAttempts(String key) {
        String normalizedKey = normalize(key);
        if (normalizedKey == null) return MAX_ATTEMPTS;

        AttemptInfo info = attemptsCache.get(normalizedKey);
        if (info == null) return MAX_ATTEMPTS;
        if (isExpired(info, System.currentTimeMillis())) {
            attemptsCache.remove(normalizedKey);
            return MAX_ATTEMPTS;
        }
        return Math.max(0, MAX_ATTEMPTS - info.attempts);
    }

    public synchronized void reset() {
        attemptsCache.clear();
    }

    private String normalize(String key) {
        if (key == null || key.isBlank()) return null;
        return key.trim().toLowerCase();
    }

    private boolean isExpired(AttemptInfo info, long now) {
        return now - info.lastAttemptTime > BLOCK_DURATION_MS;
    }
}
