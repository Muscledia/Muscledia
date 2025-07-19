package com.muscledia.api_gateway.config;

import com.muscledia.api_gateway.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints - Authentication
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health", "/gateway/**").permitAll()

                        // Public endpoints - Exercises (read-only access)
                        .requestMatchers(HttpMethod.GET, "/api/v1/exercises/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/muscle-groups/**").permitAll()

                        // Public endpoints - Workout Plans (read-only public plans)
                        .requestMatchers(HttpMethod.GET, "/api/v1/workout-plans/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/routine-folders/public/**").permitAll()

                        // Admin endpoints - require ADMIN role
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Write operations on exercises/muscle groups - require ADMIN role
                        .requestMatchers(HttpMethod.POST, "/api/v1/exercises/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/exercises/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/exercises/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/muscle-groups/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/muscle-groups/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/muscle-groups/**").hasRole("ADMIN")

                        // User-specific endpoints - require authentication
                        .requestMatchers("/api/v1/workouts/**").authenticated()
                        .requestMatchers("/api/v1/analytics/**").authenticated()
                        .requestMatchers("/api/v1/workout-plans/personal/**").authenticated()
                        .requestMatchers("/api/v1/workout-plans/my-created/**").authenticated()
                        .requestMatchers("/api/v1/routine-folders/personal/**").authenticated()

                        // Gamification endpoints - require authentication
                        .requestMatchers("/api/badges/**").authenticated()
                        .requestMatchers("/api/champions/**").authenticated()
                        .requestMatchers("/api/quests/**").authenticated()
                        .requestMatchers("/api/users/{userId}/profile").authenticated()
                        .requestMatchers("/api/users/{userId}/streaks/**").authenticated()
                        .requestMatchers("/api/users/{userId}/rank/**").authenticated()
                        .requestMatchers("/api/users/{userId}/achievements").authenticated()
                        .requestMatchers("/api/users/leaderboards/**").authenticated()

                        // General user management - require authentication
                        .requestMatchers("/api/v1/users/**").authenticated()

                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow specific origins (configure based on your frontend URLs)
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // Allow specific HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allow specific headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Allow credentials
        configuration.setAllowCredentials(true);

        // Expose specific headers to frontend
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}