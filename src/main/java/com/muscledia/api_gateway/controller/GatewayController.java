package com.muscledia.api_gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "Muscledia API Gateway");
        health.put("version", "1.0.0");
        return ResponseEntity.ok(health);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "Muscledia API Gateway");
        info.put("description", "API Gateway for Muscledia microservices");
        info.put("version", "1.0.0");
        Map<String, Object> services = new HashMap<>();
        services.put("muscledia-user-service", Map.of(
                "authentication", "/api/v1/auth/**",
                "user-management", "/api/v1/users/**"));
        services.put("muscledia-workout-service", Map.of(
                "workouts", "/api/v1/workouts/**",
                "exercises", "/api/v1/exercises/**",
                "muscle-groups", "/api/v1/muscle-groups/**",
                "workout-plans", "/api/v1/workout-plans/**",
                "analytics", "/api/v1/analytics/**",
                "admin-data", "/api/admin/data/**"));
        services.put("gamification-service", Map.of(
                "badges", "/api/badges/**",
                "champions", "/api/champions/**",
                "quests", "/api/quests/**",
                "user-gamification", "/api/users/{userId}/**"));
        info.put("services", services);
        info.put("features", new String[] {
                "JWT Authentication",
                "Request Routing",
                "Service Discovery",
                "CORS Support",
                "User Context Forwarding"
        });
        return ResponseEntity.ok(info);
    }

    @GetMapping("/routes")
    public ResponseEntity<Map<String, Object>> routes() {
        Map<String, Object> routes = new HashMap<>();
        routes.put("protected_routes", Map.of(
                "/api/v1/users/**", "User management endpoints",
                "/api/v1/workouts/**", "Workout management endpoints",
                "/api/v1/analytics/**", "Workout analytics endpoints",
                "/api/badges/**", "Badge management endpoints",
                "/api/champions/**", "Champions leaderboard endpoints",
                "/api/quests/**", "Quest system endpoints",
                "/api/users/{userId}/**", "User gamification endpoints"));
        Map<String, String> publicRoutes = new HashMap<>();
        publicRoutes.put("/api/v1/auth/login", "User login");
        publicRoutes.put("/api/v1/auth/register", "User registration");
        publicRoutes.put("/api/v1/auth/refresh", "Token refresh");
        publicRoutes.put("GET /api/v1/exercises/**", "Browse exercises (read-only)");
        publicRoutes.put("GET /api/v1/muscle-groups/**", "Browse muscle groups (read-only)");
        publicRoutes.put("GET /api/v1/workout-plans/public/**", "Public workout plans");
        publicRoutes.put("/gateway/health", "Gateway health check");
        publicRoutes.put("/gateway/info", "Gateway information");
        routes.put("public_routes", publicRoutes);
        return ResponseEntity.ok(routes);
    }
}