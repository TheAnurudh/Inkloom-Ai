package com.inkloom.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkloom.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Very small in-memory limiter for /api/auth/** (stops password guessing and sign-up spam).
 * Fixed 1-minute window per client IP. For multiple servers use Redis/a gateway instead.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final long WINDOW_MS = 60_000;

    private final int maxPerWindow;
    private final ObjectMapper mapper;
    private final Map<String, long[]> hits = new ConcurrentHashMap<>();   // ip -> [windowStart, count]

    public RateLimitFilter(int maxPerWindow, ObjectMapper mapper) {
        this.maxPerWindow = maxPerWindow;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // getRemoteAddr() cannot be spoofed by a header (X-Forwarded-For can). Behind a proxy configure forwarded headers properly.
        String ip = req.getRemoteAddr();
        long now = System.currentTimeMillis();

        long[] w = hits.compute(ip, (k, v) -> {
            if (v == null || now - v[0] > WINDOW_MS) return new long[]{now, 1};
            v[1]++;
            return v;
        });

        if (hits.size() > 10_000) hits.entrySet().removeIf(e -> now - e.getValue()[0] > WINDOW_MS);

        if (w[1] > maxPerWindow) {
            log.warn("[RATE-LIMIT] blocked ip={} path={} count={}", ip, req.getRequestURI(), w[1]);
            res.setStatus(429);
            res.setHeader("Retry-After", "60");
            res.setContentType("application/json");
            mapper.writeValue(res.getOutputStream(), new ApiError(Instant.now(), 429, "Too Many Requests",
                    "Too many requests. Try again in a minute.", req.getRequestURI(), MDC.get("rid"), null));
            return;
        }
        chain.doFilter(req, res);
    }
}
