package com.ecommerce.projetobackend.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class InMemoryRateLimiter implements RateLimiter {

    private static final int CLEANUP_INTERVAL = 1000;

    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final AtomicInteger attemptsSinceCleanup = new AtomicInteger();
    private final Clock clock;

    public InMemoryRateLimiter() {
        this(Clock.systemUTC());
    }

    InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public RateLimitResult consume(String key, int limit, Duration window) {
        long now = clock.millis();
        long windowMillis = window.toMillis();
        AtomicReference<RateLimitResult> result = new AtomicReference<>();

        counters.compute(key, (ignored, current) -> {
            WindowCounter activeWindow = current;
            if (activeWindow == null || now >= activeWindow.resetAtMillis()) {
                activeWindow = new WindowCounter(now + windowMillis, 0);
            }

            if (activeWindow.count() >= limit) {
                result.set(new RateLimitResult(false, limit, 0, activeWindow.resetAtMillis()));
                return activeWindow;
            }

            int newCount = activeWindow.count() + 1;
            result.set(new RateLimitResult(true, limit, limit - newCount, activeWindow.resetAtMillis()));
            return new WindowCounter(activeWindow.resetAtMillis(), newCount);
        });

        cleanupExpiredCounters(now);
        return result.get();
    }

    private void cleanupExpiredCounters(long now) {
        if (attemptsSinceCleanup.incrementAndGet() % CLEANUP_INTERVAL != 0) {
            return;
        }

        counters.entrySet().removeIf(entry -> entry.getValue().resetAtMillis() <= now);
    }

    private record WindowCounter(long resetAtMillis, int count) {
    }
}
