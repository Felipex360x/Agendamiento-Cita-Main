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
    public static final int MAX_REGISTERS_PER_HOUR = 10;
    private static final long REGISTER_WINDOW_MS = 60 * 60 * 1000;

    private static final class AttemptInfo {
        private int attempts;
        private long lastAttemptTime;

        private AttemptInfo(int attempts, long lastAttemptTime) {
            this.attempts = attempts;
            this.lastAttemptTime = lastAttemptTime;
        }
    }

    // Límite máximo de registros en memoria para que la lista no crezca sin control.
    private final LinkedHashMap<String, AttemptInfo> attemptsCache = new LinkedHashMap<>(256, 0.75f, true);
    private final LinkedHashMap<String, AttemptInfo> registrosPorIp = new LinkedHashMap<>(256, 0.75f, true);

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
     * Registra un intento para todas las claves antes de revisar la contraseña.
     * Si alguna clave ya está bloqueada, devuelve false.
     * Si el login sale bien, se limpia la cuenta con loginSucceeded y se libera la IP con release.
     */
    public synchronized boolean tryAcquire(String... keys) {
        for (String key : keys) {
            if (isBlocked(key)) return false;
        }
        for (String key : keys) {
            loginFailed(key); // suma el intento
        }
        return true;
    }

    /** Resta un intento guardado (cuando el login fue correcto o hubo un error del servidor). */
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

    /**
     * Cuenta una solicitud de registro desde una IP y dice si ya superó el límite por hora.
     * Evita crear cuentas en masa y usar el correo de bienvenida para enviar spam.
     */
    public synchronized boolean registroBloqueado(String ip) {
        String key = normalize(ip);
        if (key == null) return false;

        long now = System.currentTimeMillis();
        AttemptInfo info = registrosPorIp.get(key);
        if (info == null || now - info.lastAttemptTime > REGISTER_WINDOW_MS) {
            // lastAttemptTime guarda aquí el inicio de la ventana de una hora.
            registrosPorIp.put(key, new AttemptInfo(1, now));
        } else {
            info.attempts++;
        }
        while (registrosPorIp.size() > MAX_TRACKED_KEYS) {
            Iterator<Map.Entry<String, AttemptInfo>> oldest = registrosPorIp.entrySet().iterator();
            oldest.next();
            oldest.remove();
        }
        return registrosPorIp.get(key).attempts > MAX_REGISTERS_PER_HOUR;
    }

    public synchronized void reset() {
        attemptsCache.clear();
        registrosPorIp.clear();
    }

    private String normalize(String key) {
        if (key == null || key.isBlank()) return null;
        return key.trim().toLowerCase();
    }

    private boolean isExpired(AttemptInfo info, long now) {
        return now - info.lastAttemptTime > BLOCK_DURATION_MS;
    }
}
