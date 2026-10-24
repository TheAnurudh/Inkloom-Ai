package com.inkloom.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkloom.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/** Logged-in user but not allowed -> JSON 403. */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RestAccessDeniedHandler.class);
    private final ObjectMapper mapper;

    public RestAccessDeniedHandler(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        log.warn("[SECURITY] 403 {} {}", req.getMethod(), req.getRequestURI());
        res.setStatus(403);
        res.setContentType("application/json");
        mapper.writeValue(res.getOutputStream(), new ApiError(Instant.now(), 403, "Forbidden",
                "You do not have permission to do this", req.getRequestURI(), MDC.get("rid"), null));
    }
}
