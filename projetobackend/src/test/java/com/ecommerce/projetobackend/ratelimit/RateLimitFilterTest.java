package com.ecommerce.projetobackend.ratelimit;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitFilterTest {

    private static final long RESET_AT_MILLIS = System.currentTimeMillis() + Duration.ofMinutes(1).toMillis();

    private final RateLimitProperties properties = properties();
    private final FakeRateLimiter rateLimiter = new FakeRateLimiter();
    private final RateLimitFilter filter = new RateLimitFilter(
            properties,
            rateLimiter,
            new ObjectMapper()
    );

    @Test
    void appliesConfiguredRuleAndContinuesWhenRequestIsAllowed() throws Exception {
        rateLimiter.nextResult = new RateLimitResult(true, 5, 4, RESET_AT_MILLIS);
        MockHttpServletRequest request = request("POST", "/api/v1/auth/login");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.5");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter.doFilter(request, response, chain);

        assertTrue(chain.invoked);
        assertEquals(1, rateLimiter.calls);
        assertEquals("auth-login:203.0.113.10", rateLimiter.lastKey);
        assertEquals(5, rateLimiter.lastLimit);
        assertEquals(Duration.ofMinutes(1), rateLimiter.lastWindow);
        assertEquals("5", response.getHeader("X-RateLimit-Limit"));
        assertEquals("4", response.getHeader("X-RateLimit-Remaining"));
        assertEquals(String.valueOf(RESET_AT_MILLIS / 1000), response.getHeader("X-RateLimit-Reset"));
    }

    @Test
    void returnsTooManyRequestsWhenLimitIsExceeded() throws Exception {
        rateLimiter.nextResult = new RateLimitResult(false, 5, 0, RESET_AT_MILLIS);
        MockHttpServletRequest request = request("POST", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter.doFilter(request, response, chain);

        assertFalse(chain.invoked);
        assertEquals(429, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals("5", response.getHeader("X-RateLimit-Limit"));
        assertEquals("0", response.getHeader("X-RateLimit-Remaining"));
        assertTrue(Integer.parseInt(response.getHeader(HttpHeaders.RETRY_AFTER)) >= 1);
        assertTrue(response.getContentAsString().contains("\"error\":\"Too Many Requests\""));
        assertTrue(response.getContentAsString().contains("\"path\":\"/api/v1/auth/login\""));
    }

    @Test
    void skipsLimiterWhenRequestDoesNotMatchAnyRule() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter.doFilter(request, response, chain);

        assertTrue(chain.invoked);
        assertEquals(0, rateLimiter.calls);
    }

    @Test
    void skipsLimiterWhenRateLimitIsDisabled() throws Exception {
        properties.setEnabled(false);
        MockHttpServletRequest request = request("POST", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter.doFilter(request, response, chain);

        assertTrue(chain.invoked);
        assertEquals(0, rateLimiter.calls);
    }

    private RateLimitProperties properties() {
        RateLimitProperties.Rule loginRule = new RateLimitProperties.Rule();
        loginRule.setName("auth-login");
        loginRule.setPathPattern("/api/v1/auth/login");
        loginRule.setMethods(Set.of("POST"));
        loginRule.setLimit(5);
        loginRule.setWindow(Duration.ofMinutes(1));

        RateLimitProperties rateLimitProperties = new RateLimitProperties();
        rateLimitProperties.setEnabled(true);
        rateLimitProperties.setClientIdHeader("X-Forwarded-For");
        rateLimitProperties.setRules(List.of(loginRule));
        return rateLimitProperties;
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    private static class FakeRateLimiter implements RateLimiter {

        private RateLimitResult nextResult = new RateLimitResult(true, 5, 4, RESET_AT_MILLIS);
        private int calls;
        private String lastKey;
        private int lastLimit;
        private Duration lastWindow;

        @Override
        public RateLimitResult consume(String key, int limit, Duration window) {
            calls++;
            lastKey = key;
            lastLimit = limit;
            lastWindow = window;
            return nextResult;
        }
    }

    private static class RecordingFilterChain implements FilterChain {

        private boolean invoked;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            invoked = true;
        }
    }
}
