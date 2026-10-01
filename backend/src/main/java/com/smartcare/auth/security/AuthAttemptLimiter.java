package com.smartcare.auth.security;

import com.smartcare.common.error.TooManyRequestsException;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;

/** Bounded single-instance limiter. Production ingress must enforce distributed IP limits too. */
@Component
public class AuthAttemptLimiter {
    private record Window(Instant expires, int count) {}
    private final Map<String, Window> windows = new HashMap<>();
    private final Clock clock;
    public AuthAttemptLimiter(Clock clock) { this.clock = clock; }
    public synchronized void check(String key) {
        Instant now = clock.instant();
        windows.entrySet().removeIf(e -> !e.getValue().expires().isAfter(now));
        Window window = windows.get(key);
        if (window == null && windows.size() >= 10000) throw new TooManyRequestsException("Please try again later.");
        if (window != null && window.count() >= 10) throw new TooManyRequestsException("Too many attempts. Try again in 15 minutes.");
        windows.put(key, new Window(window == null ? now.plusSeconds(900) : window.expires(), window == null ? 1 : window.count() + 1));
    }
    public synchronized void clear(String key) { windows.remove(key); }
}
