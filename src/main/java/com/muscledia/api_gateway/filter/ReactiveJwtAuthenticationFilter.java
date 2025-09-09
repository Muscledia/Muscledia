package com.muscledia.api_gateway.filter;

import com.muscledia.api_gateway.config.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

//@Component
public class ReactiveJwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ReactiveJwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.header}")
    private String tokenHeader;

    @Value("$jwt.prefix}")
    private String tokenPrefix;

    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/users/login",
            "/api/users/register",
            "/actuator/health",
            "/gateway/health"
    );

    private static final List<String> PUBLIC_READ_PATHS = List.of(
            "/api/v1/exercises",
            "/api/v1/muscle-groups",
            "/api/v1/workout-plans/public",
            "/api/v1/routine-folders/public",
            "/api/gamification/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        // Log incoming request
        // Add this line for debugging
        logger.info("Gateway processing: {} {} from {}", method, path, request.getRemoteAddress());

        // Skip JWT validation for excluded paths
        if (isExcludedPath(path)) {
            logger.debug("Skipping JWT validation for excluded path: {}", path);
            return chain.filter(exchange);
        }

        // Skip JWT validation for public read-only endpoints (GET requests)
        if ("GET".equals(method) && isPublicReadPath(path)) {
            logger.debug("Skipping JWT validation for public read path: {}", path);
            return chain.filter(exchange);
        }

        // Extract JWT token
        String authHeader = request.getHeaders().getFirst(tokenHeader);

        if (authHeader == null || !authHeader.startsWith(tokenPrefix)) {
            logger.warn("Missing or invalid Authorization header for path: {}", path);
            return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(tokenPrefix.length());

        try {
            if (!jwtUtil.validateToken(token)) {
                logger.warn("Invalid JWT token for path: {}", path);
                return onError(exchange, "Invalid or expired JWT token", HttpStatus.UNAUTHORIZED);
            }

            // Extract user info
            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);
            Long userId = jwtUtil.extractUserId(token);

            logger.debug("Authenticated user: {} (ID: {}, Role: {})", username, userId, role);

            // Add user info to request headers for downstream services
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId != null ? userId.toString() : "0")
                    .header("X-Username", username != null ? username : "anonymous")
                    .header("X-User-Role", role != null ? role : "USER")
                    .header("X-Auth-Source", "gateway")
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            logger.error("JWT token processing error for path: {}", path, e);
            return onError(exchange, "JWT token processing error", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean isPublicReadPath(String path) {
        return PUBLIC_READ_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String errorMessage, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        String body = String.format(
                "{\"error\":\"%s\",\"status\":%d,\"timestamp\":\"%s\",\"path\":\"%s\"}",
                errorMessage,
                httpStatus.value(),
                java.time.Instant.now().toString(),
                exchange.getRequest().getURI().getPath()
        );

        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -1; // Execute before other filters
    }
}
