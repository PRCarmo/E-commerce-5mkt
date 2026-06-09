package com.ecommerce.projetobackend.ratelimit;

public record RateLimitResult(
        boolean allowed,
        int limit,
        int remaining,
        long resetAtMillis
) {
}
