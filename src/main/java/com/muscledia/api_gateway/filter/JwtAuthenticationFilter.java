package com.muscledia.api_gateway.filter;

import com.muscledia.api_gateway.config.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@Order(1) // Execute first
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${application.security.jwt.header}")
    private String tokenHeader;

    @Value("${application.security.jwt.prefix}")
    private String tokenPrefix;

    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/actuator/health",
            "/gateway/health",
            "/gateway/info",
            "/gateway/routes");

    private static final List<String> PUBLIC_READ_PATHS = List.of(
            "/api/v1/exercises",
            "/api/v1/muscle-groups",
            "/api/v1/workout-plans/public",
            "/api/v1/routine-folders/public");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        // Skip JWT validation for excluded paths
        if (isExcludedPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip JWT validation for public read-only endpoints (GET requests)
        if ("GET".equals(method) && isPublicReadPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authToken = request.getHeader(tokenHeader);
        String username = null;

        if (authToken != null && authToken.startsWith(tokenPrefix)) {
            authToken = authToken.substring(tokenPrefix.length());
            try {
                username = jwtUtil.extractUsername(authToken);
            } catch (Exception e) {
                logger.error("Cannot get the username from token", e);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Invalid JWT token\"}");
                return;
            }
        } else {
            logger.warn("JWT Token does not begin with Bearer String");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Missing or invalid Authorization header\"}");
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtil.validateToken(authToken)) {
                String role = jwtUtil.extractRole(authToken);
                Long userId = jwtUtil.extractUserId(authToken);

                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        username, null, Collections.singletonList(authority));

                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Add user information to request headers for downstream services
                request.setAttribute("userId", userId);
                request.setAttribute("username", username);
                request.setAttribute("role", role);

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            } else {
                logger.error("JWT token is not valid");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"JWT token is expired or invalid\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean isPublicReadPath(String path) {
        return PUBLIC_READ_PATHS.stream().anyMatch(path::startsWith);
    }
}