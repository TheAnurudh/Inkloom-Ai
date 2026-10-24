package com.inkloom.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkloom.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/** Called when someone reaches a protected URL without a valid token -> JSON 401. */
@Component
public class RestAuthEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(RestAuthEntryPoint.class);
    private final ObjectMapper mapper;

    public RestAuthEntryPoint(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        log.warn("[SECURITY] 401 {} {} (missing/invalid token)", req.getMethod(), req.getRequestURI());
        res.setStatus(401);
        res.setContentType("application/json");
        mapper.writeValue(res.getOutputStream(), new ApiError(Instant.now(), 401, "Unauthorized",
                "Authentication required. Send header: Authorization: Bearer <token>", req.getRequestURI(), MDC.get("rid"), null));
    }
}
