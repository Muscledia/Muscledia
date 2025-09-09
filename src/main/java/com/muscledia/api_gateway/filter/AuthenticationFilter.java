package com.muscledia.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Jwts;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory <AuthenticationFilter.Config>{
    @Value("${jwt.secret}")
    private String jwtSecret;


    public AuthenticationFilter() {
        super(Config.class);
    }


    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            log.debug("AuthenticationFilter applied with config: enabled={}", config.isEnabled());

            // Check if filter is enabled
            if (!config.isEnabled()) {
                log.debug("Authentication filter is disabled, skipping...");
                return chain.filter(exchange);
            }

            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().toString();

            log.debug("Processing request: {} {}", request.getMethod(), path);

            // Skip authentication for public endpoints
            if (isPublicEndpoint(path, config)) {
                log.debug("Public endpoint detected, skipping authentication: {}", path);
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(config.getHeaderName());

            if (authHeader == null || !authHeader.startsWith(config.getTokenPrefix())) {
                log.warn("Missing or invalid Authorization header for path: {}", path);
                return unauthorizedResponse(exchange, "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(config.getTokenPrefix().length());

            try {
                Claims claims = validateToken(token, config);
                log.debug("JWT validation successful for user: {}", claims.getSubject());

                // Check required role if configured
                if (config.isRequireRole()) {
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) claims.get("roles");
                    if (roles == null || !roles.contains(config.getRequiredRole())) {
                        log.warn("User {} does not have required role: {}", claims.getSubject(), config.getRequiredRole());
                        return unauthorizedResponse(exchange, "Insufficient permissions");
                    }
                    log.debug("Role validation successful for user: {}", claims.getSubject());
                }

                // Add user context to request headers for downstream services
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", String.valueOf(claims.get("userId")))
                        .header("X-Username", claims.getSubject())
                        .header("X-User-Roles", String.join(",", (List<String>) claims.get("roles")))
                        .build();

                log.debug("Request headers added: X-User-Id={}, X-Username={}",
                        claims.get("userId"), claims.getSubject());

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                log.error("JWT validation failed for path {}: {}", path, e.getMessage());
                return unauthorizedResponse(exchange, "Invalid or expired token");
            }
        };
    }

    private Claims validateToken(String token, Config config) {
        log.info("=== JWT VALIDATION START ===");
        log.info("Token length: {}", token.length());
        log.info("JWT Secret length: {}", jwtSecret.length());
        log.info("Using secret: {}...", jwtSecret.substring(0, Math.min(20, jwtSecret.length())));
        try{
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            log.info("Secret key created successfully");


            var parserBuilder = Jwts.parser().verifyWith(key);
            log.info("Parser builder created");

            Claims claims = parserBuilder.build().parseSignedClaims(token).getPayload();
            log.info("=== JWT VALIDATION SUCCESS ===");
            log.info("Subject: {}", claims.getSubject());
            log.info("User ID: {}", claims.get("userId"));
            log.info("Roles: {}", claims.get("roles"));

            return claims;
        }catch(Exception e){
            log.error("=== JWT VALIDATION FAILED ===");
            log.error("Error type: {}", e.getClass().getSimpleName());
            log.error("Error message: {}", e.getMessage());
            throw e;
        }

    }

    private boolean isPublicEndpoint(String path, Config config) {
        if (!config.isSkipPublicPaths()) {
            return false;
        }

        return config.getPublicPaths().stream()
                .anyMatch(path::contains);
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");

        String body = String.format(
                "{\"error\": \"Unauthorized\", \"message\": \"%s\", \"timestamp\": \"%s\"}",
                message, java.time.Instant.now()
        );

        log.debug("Returning 401 Unauthorized: {}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Setter
    @Getter
    public static class Config {
        // Getters and setters
        private boolean enabled = true;
        private String headerName = "Authorization";
        private String tokenPrefix = "Bearer ";
        private boolean validateExpiration = true;
        private boolean requireRole = false;
        private String requiredRole = "USER";
        private boolean skipPublicPaths = true;
        private List<String> publicPaths = List.of(
                "/login", "/register", "/public", "/health", "/actuator", "/swagger", "/api-docs"
        );

    }
}
