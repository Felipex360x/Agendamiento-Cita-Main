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
