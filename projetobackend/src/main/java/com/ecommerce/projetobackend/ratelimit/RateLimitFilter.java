package com.ecommerce.projetobackend.ratelimit;

import com.ecommerce.projetobackend.shared.exception.ErrorResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RateLimitFilter(RateLimitProperties properties,
                           RateLimiter rateLimiter,
                           ObjectMapper objectMapper) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitProperties.Rule rule = findMatchingRule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitResult result = rateLimiter.consume(
                buildKey(rule, request),
                rule.getLimit(),
                rule.getWindow()
        );

        addRateLimitHeaders(response, result);

        if (result.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        writeTooManyRequestsResponse(request, response);
    }

    private RateLimitProperties.Rule findMatchingRule(HttpServletRequest request) {
        if (properties.getRules() == null || properties.getRules().isEmpty()) {
            return null;
        }

        String path = requestPath(request);
        String method = request.getMethod().toUpperCase(Locale.ROOT);

        return properties.getRules().stream()
                .filter(this::isValid)
                .filter(rule -> matchesMethod(rule.getMethods(), method))
                .filter(rule -> pathMatcher.match(rule.getPathPattern(), path))
                .findFirst()
                .orElse(null);
    }

    private boolean isValid(RateLimitProperties.Rule rule) {
        Duration window = rule.getWindow();
        return rule.getName() != null
                && rule.getPathPattern() != null
                && rule.getLimit() > 0
                && window != null
                && !window.isZero()
                && !window.isNegative();
    }

    private boolean matchesMethod(Set<String> methods, String requestMethod) {
        return methods == null
                || methods.isEmpty()
                || methods.stream()
                .filter(Objects::nonNull)
                .map(method -> method.toUpperCase(Locale.ROOT))
                .anyMatch(requestMethod::equals);
    }

    private String buildKey(RateLimitProperties.Rule rule, HttpServletRequest request) {
        return rule.getName() + ":" + clientId(request);
    }

    private String clientId(HttpServletRequest request) {
        String headerName = properties.getClientIdHeader();
        String headerValue = headerName == null ? null : request.getHeader(headerName);
        if (headerValue != null && !headerValue.isBlank()) {
            String firstForwardedValue = headerValue.split(",")[0].trim();
            if (!firstForwardedValue.isBlank()) {
                return firstForwardedValue;
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? "unknown-client" : remoteAddr;
    }

    private String requestPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();

        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }

        return requestUri;
    }

    private void addRateLimitHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.resetAtMillis() / 1000));

        if (!result.allowed()) {
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds(result)));
        }
    }

    private void writeTooManyRequestsResponse(HttpServletRequest request,
                                              HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("Too Many Requests")
                .message("Too many requests. Try again later.")
                .path(request.getRequestURI())
                .build();

        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private long retryAfterSeconds(RateLimitResult result) {
        long millisUntilReset = result.resetAtMillis() - System.currentTimeMillis();
        return Math.max(1, (long) Math.ceil(millisUntilReset / 1000.0));
    }
}
