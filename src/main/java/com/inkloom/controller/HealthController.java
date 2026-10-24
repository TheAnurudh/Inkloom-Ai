package com.inkloom.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/** Public ping: open http://localhost:8080/api/health to check the server is alive. */
@RestController
public class HealthController {
    @GetMapping("/api/health")
    public Map<String, Object> health() { return Map.of("status", "UP", "time", Instant.now().toString()); }
}
