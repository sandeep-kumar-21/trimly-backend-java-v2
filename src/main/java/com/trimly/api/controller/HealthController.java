package com.trimly.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Application and infrastructure health monitoring")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping
    @Operation(summary = "Health check endpoint for uptime and monitoring services")
    @ApiResponse(responseCode = "200", description = "Application services health status.")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        String dbStatus = "disconnected";
        String redisStatus = "disconnected";

        // Check PostgreSQL
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result != null && result == 1) {
                dbStatus = "connected";
            }
        } catch (Exception ex) {
            log.warn("Health check - PostgreSQL connection error: {}", ex.getMessage());
            dbStatus = "error";
        }

        // Check Redis
        try {
            if (redisTemplate.getConnectionFactory() != null) {
                try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
                    String ping = connection.ping();
                    if ("PONG".equalsIgnoreCase(ping)) {
                        redisStatus = "connected";
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Health check - Redis connection error: {}", ex.getMessage());
            redisStatus = "error";
        }

        boolean isHealthy = "connected".equals(dbStatus) && "connected".equals(redisStatus);

        Map<String, Object> response = new HashMap<>();
        response.put("status", isHealthy ? "ok" : "degraded");
        response.put("timestamp", Instant.now().toString());
        response.put("services", Map.of(
                "database", dbStatus,
                "redis", redisStatus
        ));

        return ResponseEntity.ok(response);
    }
}
