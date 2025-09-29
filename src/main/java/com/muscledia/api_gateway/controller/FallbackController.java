package com.muscledia.api_gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/workout-service")
    public ResponseEntity<Map<String, Object>> workoutServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Workout service is currently unavailable",
                        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "timestamp", Instant.now().toString(),
                        "service", "workout-service"
                ));
    }

    @GetMapping("/gamification-service")
    public ResponseEntity<Map<String, Object>> gamificationServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Gamification service is currently unavailable",
                        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "timestamp", Instant.now().toString(),
                        "service", "gamification-service"
                ));
    }

    @GetMapping("/user-service")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "User service is currently unavailable",
                        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "timestamp", Instant.now().toString(),
                        "service", "user-service"
                ));
    }
}
