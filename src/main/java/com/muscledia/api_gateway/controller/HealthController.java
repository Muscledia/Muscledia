package com.muscledia.api_gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/gateway")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString(),
                "service", "Muscledia API Gateway",
                "version", "2.0.0"
        ));
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
                "name", "Muscledia API Gateway",
                "description", "Production-ready API Gateway for Muscledia microservices",
                "version", "2.0.0",
                "features", new String[] {
                        "JWT Authentication",
                        "Rate Limiting",
                        "Circuit Breakers",
                        "Service Discovery",
                        "CORS Support",
                        "Request Monitoring"
                }
        ));
    }

    @GetMapping("/routes")
    public Mono<ResponseEntity<String>> getRoutes() {
        return Mono.just(ResponseEntity.ok("Check logs for route information"));
    }
}
