package com.ethsimulator.web;

import com.ethsimulator.api.error.ErrorResponse;
import com.ethsimulator.config.EthSimulatorProperties;
import io.micrometer.core.instrument.Counter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final List<String> LIMITED_PREFIXES = List.of(
            "/api/audit/",
            "/api/wallet/",
            "/agent/"
    );

    private final EthSimulatorProperties properties;
    private final RateLimiter rateLimiter;
    private final Map<String, Counter> rejectionCounters = new ConcurrentHashMap<>();

    public RateLimitFilter(EthSimulatorProperties properties, RateLimiter rateLimiter) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isRateLimitEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return LIMITED_PREFIXES.stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String clientKey = resolveClientKey(request);
        if (rateLimiter.tryAcquire(clientKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfter = rateLimiter.retryAfterSeconds(clientKey);
        rejectionCounterFor(prefixFor(request.getRequestURI())).increment();
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(retryAfter));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ErrorResponse body = new ErrorResponse("RATE_LIMITED", "Too many requests", List.of());
        response.getWriter().write(MAPPER.writeValueAsString(body));
    }

    private String resolveClientKey(HttpServletRequest request) {
        if (properties.isRateLimitTrustForwardedFor()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                int comma = forwarded.indexOf(',');
                return (comma >= 0 ? forwarded.substring(0, comma) : forwarded).trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String prefixFor(String path) {
        for (String prefix : LIMITED_PREFIXES) {
            if (path.startsWith(prefix)) {
                return prefix;
            }
        }
        return "other";
    }

    private Counter rejectionCounterFor(String prefix) {
        return rejectionCounters.computeIfAbsent(prefix, key ->
                io.micrometer.core.instrument.Metrics.counter(
                        "ethsim_ratelimit_rejections_total",
                        "path_prefix",
                        key
                ));
    }
}