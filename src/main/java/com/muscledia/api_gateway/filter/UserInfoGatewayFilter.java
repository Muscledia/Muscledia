package com.muscledia.api_gateway.filter;

import com.muscledia.api_gateway.config.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(2) // Execute after JWT authentication filter
public class UserInfoGatewayFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${application.security.jwt.header}")
    private String tokenHeader;

    @Value("${application.security.jwt.prefix}")
    private String tokenPrefix;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(tokenHeader);

        if (authHeader != null && authHeader.startsWith(tokenPrefix)) {
            String token = authHeader.substring(tokenPrefix.length());
            try {
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);
                Long userId = jwtUtil.extractUserId(token);

                // Add user information headers for downstream services
                response.addHeader("X-User-Id", userId != null ? userId.toString() : "0");
                response.addHeader("X-Username", username != null ? username : "anonymous");
                response.addHeader("X-User-Role", role != null ? role : "USER");
                response.addHeader("X-User-Info",
                        String.format("userId=%d;username=%s;role=%s",
                                userId != null ? userId : 0,
                                username != null ? username : "anonymous",
                                role != null ? role : "USER"));

            } catch (Exception e) {
                // If token parsing fails, continue with anonymous headers
                response.addHeader("X-User-Id", "0");
                response.addHeader("X-Username", "anonymous");
                response.addHeader("X-User-Role", "USER");
                response.addHeader("X-User-Info", "userId=0;username=anonymous;role=USER");
            }
        } else {
            // No valid token, set anonymous headers
            response.addHeader("X-User-Id", "0");
            response.addHeader("X-Username", "anonymous");
            response.addHeader("X-User-Role", "USER");
            response.addHeader("X-User-Info", "userId=0;username=anonymous;role=USER");
        }

        filterChain.doFilter(request, response);
    }
}