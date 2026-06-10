package com.ecommerce.projetobackend.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRateLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    private final InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter(clock);

    @Test
    void blocksRequestsAfterLimitIsReached() {
        RateLimitResult first = rateLimiter.consume("auth-login:127.0.0.1", 2, Duration.ofMinutes(1));
        RateLimitResult second = rateLimiter.consume("auth-login:127.0.0.1", 2, Duration.ofMinutes(1));
        RateLimitResult third = rateLimiter.consume("auth-login:127.0.0.1", 2, Duration.ofMinutes(1));

        assertTrue(first.allowed());
        assertEquals(1, first.remaining());
        assertTrue(second.allowed());
        assertEquals(0, second.remaining());
        assertFalse(third.allowed());
        assertEquals(0, third.remaining());
    }

    @Test
    void allowsRequestsAgainAfterWindowExpires() {
        rateLimiter.consume("auth-login:127.0.0.1", 1, Duration.ofMinutes(1));
        RateLimitResult blocked = rateLimiter.consume("auth-login:127.0.0.1", 1, Duration.ofMinutes(1));

        clock.advance(Duration.ofMinutes(1));
        RateLimitResult allowedAfterReset = rateLimiter.consume("auth-login:127.0.0.1", 1, Duration.ofMinutes(1));

        assertFalse(blocked.allowed());
        assertTrue(allowedAfterReset.allowed());
        assertEquals(0, allowedAfterReset.remaining());
    }

    private static class MutableClock extends Clock {

        private Instant currentInstant;

        private MutableClock(Instant currentInstant) {
            this.currentInstant = currentInstant;
        }

        private void advance(Duration duration) {
            currentInstant = currentInstant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }
    }
}
