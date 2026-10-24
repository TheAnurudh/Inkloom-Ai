package com.inkloom.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * DEBUG HELPER. Gives each request a short id (rid), shown in every log line and returned in the X-Request-Id header,
 * then prints one summary line per request. Bodies/headers are NEVER logged (they contain passwords and tokens).
 */
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String rid = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("rid", rid);
        res.setHeader("X-Request-Id", rid);
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(req, res);
        } finally {
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            boolean real = a != null && a.isAuthenticated() && !(a instanceof AnonymousAuthenticationToken);
            log.info("[HTTP] {} {} -> {} ({} ms) user={} ip={}", req.getMethod(), req.getRequestURI(),
                    res.getStatus(), System.currentTimeMillis() - start, real ? a.getName() : "anonymous", req.getRemoteAddr());
            MDC.remove("rid");
        }
    }
}
