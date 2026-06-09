package com.ecommerce.projetobackend.ratelimit;

import java.time.Duration;

public interface RateLimiter {

    RateLimitResult consume(String key, int limit, Duration window);
}
