package com.raahmediq.ai.service;

import com.raahmediq.common.error.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiQueryRateLimiter {
    private final Map<UUID, Deque<Instant>> requests = new ConcurrentHashMap<>();
    private final int requestsPerMinute;
    private final Clock clock;

    public AiQueryRateLimiter(@Value("${raahmediq.ai.requests-per-minute:12}") int requestsPerMinute, Clock clock) {
        this.requestsPerMinute = requestsPerMinute;
        this.clock = clock;
    }

    public void check(UUID userId) {
        Instant cutoff = clock.instant().minus(1, ChronoUnit.MINUTES);
        Deque<Instant> bucket = requests.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        synchronized (bucket) {
            while (!bucket.isEmpty() && bucket.peekFirst().isBefore(cutoff)) bucket.removeFirst();
            if (bucket.size() >= requestsPerMinute) {
                throw new TooManyRequestsException("Please wait briefly before asking another care-record question.");
            }
            bucket.addLast(clock.instant());
        }
    }
}
