# 🔐 Complete JWT Authentication & Authorization Guide for Muscledia Microservices

## 🏗️ **Architecture Overview**

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────────┐
│   Frontend      │───▶│   API Gateway    │───▶│  User Service       │
│   (React/Web)   │    │   (Port 8080)    │    │  (Auth Provider)    │
└─────────────────┘    │                  │    └─────────────────────┘
                       │  [JWT Validation] │              │
                       │  [Route & Auth]   │              │ JWT Generation
                       │                  │              ▼
                       │                  │    ┌─────────────────────┐
                       │                  ├───▶│  Workout Service    │
                       │                  │    │  (JWT Consumer)     │
                       │                  │    └─────────────────────┘
                       │                  │              │
                       │                  │              │ JWT Validation
                       │                  │              ▼
                       │                  ├───▶┌─────────────────────┐
                       │                  │    │ Gamification Service│
                       └──────────────────┘    │  (JWT Consumer)     │
                                              └─────────────────────┘
```

## 🚀 **Step-by-Step Implementation Guide**

### **Phase 1: Shared JWT Configuration Library**

First, create a shared library that all services can use:

#### 1.1 Create `muscledia-common` Module

```xml
<!-- pom.xml for shared library -->
<groupId>com.muscledia</groupId>
<artifactId>muscledia-common</artifactId>
<version>1.0.0</version>
<packaging>jar</packaging>

<dependencies>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.6</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.6</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.6</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
</dependencies>
```

#### 1.2 JWT Utility Class (Shared)

```java
// src/main/java/com/muscledia/common/jwt/JwtUtil.java
@Component
public class JwtUtil {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration:86400000}")
    private long jwtExpiration;

    public String generateToken(UserDetails userDetails, Long userId, String role) {
        return generateToken(new HashMap<>(), userDetails, userId, role);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails, Long userId, String role) {
        extraClaims.put("userId", userId);
        extraClaims.put("role", role);
        extraClaims.put("authorities", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // ... other utility methods (extractUsername, extractClaim, etc.)
}
```

### **Phase 2: User Service - JWT Token Generation**

#### 2.1 User Service Dependencies

```xml
<!-- Add to User Service pom.xml -->
<dependency>
    <groupId>com.muscledia</groupId>
    <artifactId>muscledia-common</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### 2.2 Authentication Controller

```java
// UserService: AuthController.java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }
}
```

#### 2.3 Authentication Service

```java
// UserService: AuthenticationService.java
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        var user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        var savedUser = userRepository.save(user);
        var jwtToken = jwtUtil.generateToken(savedUser, savedUser.getId(), savedUser.getRole().name());
        var refreshToken = jwtUtil.generateRefreshToken(savedUser);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .user(UserDto.from(savedUser))
                .build();
    }

    public AuthResponse authenticate(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow();

        var jwtToken = jwtUtil.generateToken(user, user.getId(), user.getRole().name());
        var refreshToken = jwtUtil.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .user(UserDto.from(user))
                .build();
    }
}
```

#### 2.4 User Service Security Configuration

```java
// UserService: SecurityConfiguration.java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

### **Phase 3: Workout Service - JWT Validation**

#### 3.1 Workout Service Dependencies

```xml
<!-- Add to Workout Service pom.xml -->
<dependency>
    <groupId>com.muscledia</groupId>
    <artifactId>muscledia-common</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### 3.2 JWT Authentication Filter for Workout Service

```java
// WorkoutService: JwtAuthenticationFilter.java
@Component
@Order(1)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Check for user info headers from API Gateway
        String userIdHeader = request.getHeader("X-User-Id");
        String usernameHeader = request.getHeader("X-Username");
        String roleHeader = request.getHeader("X-User-Role");

        if (userIdHeader != null && usernameHeader != null) {
            // Trust the API Gateway's validation
            Long userId = Long.parseLong(userIdHeader);

            UserPrincipal userPrincipal = new UserPrincipal(
                    userId, usernameHeader, roleHeader
            );

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userPrincipal, null, getAuthorities(roleHeader)
                    );

            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }

    private Collection<? extends GrantedAuthority> getAuthorities(String role) {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
```

#### 3.3 User Principal Class

```java
// WorkoutService: UserPrincipal.java
@Data
@AllArgsConstructor
public class UserPrincipal {
    private Long userId;
    private String username;
    private String role;

    public boolean hasRole(String role) {
        return this.role.equals(role);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(this.role);
    }
}
```

#### 3.4 Workout Service Security Configuration

```java
// WorkoutService: SecurityConfig.java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        // Public read-only endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/exercises/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/muscle-groups/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/workout-plans/public/**").permitAll()

                        // Admin endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/exercises/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/exercises/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/exercises/**").hasRole("ADMIN")

                        // Protected endpoints
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

#### 3.5 Workout Controller with User Context

```java
// WorkoutService: WorkoutController.java
@RestController
@RequestMapping("/api/v1/workouts")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutService workoutService;

    @GetMapping
    public ResponseEntity<List<WorkoutDto>> getUserWorkouts(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        List<WorkoutDto> workouts = workoutService.getUserWorkouts(user.getUserId());
        return ResponseEntity.ok(workouts);
    }

    @PostMapping
    public ResponseEntity<WorkoutDto> createWorkout(
            @RequestBody CreateWorkoutRequest request,
            Authentication authentication
    ) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        WorkoutDto workout = workoutService.createWorkout(request, user.getUserId());
        return ResponseEntity.ok(workout);
    }

    @PreAuthorize("@workoutService.isWorkoutOwner(#workoutId, authentication.principal.userId)")
    @PutMapping("/{workoutId}")
    public ResponseEntity<WorkoutDto> updateWorkout(
            @PathVariable Long workoutId,
            @RequestBody UpdateWorkoutRequest request,
            Authentication authentication
    ) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        WorkoutDto workout = workoutService.updateWorkout(workoutId, request, user.getUserId());
        return ResponseEntity.ok(workout);
    }
}
```

### **Phase 4: Gamification Service - JWT Validation**

#### 4.1 Similar Setup to Workout Service

```java
// GamificationService: Same JWT filter and security config pattern
// Focus on user-specific gamification data

@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    @GetMapping("/user/{userId}")
    @PreAuthorize("#userId == authentication.principal.userId or hasRole('ADMIN')")
    public ResponseEntity<List<BadgeDto>> getUserBadges(@PathVariable Long userId) {
        return ResponseEntity.ok(badgeService.getUserBadges(userId));
    }

    @PostMapping("/{badgeId}/award/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> awardBadge(@PathVariable Long badgeId, @PathVariable Long userId) {
        badgeService.awardBadge(badgeId, userId);
        return ResponseEntity.ok().build();
    }
}
```

### **Phase 5: Environment Configuration**

#### 5.1 Shared Configuration (application.yml)

```yaml
# Common configuration for all services
application:
  security:
    jwt:
      secret-key: ${JWT_SECRET:1e5bf199c6450231fdd242f60485d0caadc32aa27a0d89da7174aed38326a879}
      expiration: ${JWT_EXPIRATION:86400000} # 24 hours
      refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800000} # 7 days

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}
```

#### 5.2 Docker Compose for Development

```yaml
# docker-compose.yml
version: "3.8"
services:
  eureka-server:
    image: your-eureka-server
    ports:
      - "8761:8761"
    environment:
      - SPRING_PROFILES_ACTIVE=docker

  user-service:
    build: ./muscledia-user-service
    ports:
      - "8081:8081"
    environment:
      - JWT_SECRET=1e5bf199c6450231fdd242f60485d0caadc32aa27a0d89da7174aed38326a879
      - EUREKA_URL=http://eureka-server:8761/eureka/
    depends_on:
      - eureka-server

  workout-service:
    build: ./muscledia-workout-service
    ports:
      - "8082:8082"
    environment:
      - JWT_SECRET=1e5bf199c6450231fdd242f60485d0caadc32aa27a0d89da7174aed38326a879
      - EUREKA_URL=http://eureka-server:8761/eureka/
    depends_on:
      - eureka-server

  gamification-service:
    build: ./gamification-service
    ports:
      - "8083:8083"
    environment:
      - JWT_SECRET=1e5bf199c6450231fdd242f60485d0caadc32aa27a0d89da7174aed38326a879
      - EUREKA_URL=http://eureka-server:8761/eureka/
    depends_on:
      - eureka-server

  api-gateway:
    build: ./muscledia-api-gateway
    ports:
      - "8080:8080"
    environment:
      - JWT_SECRET=1e5bf199c6450231fdd242f60485d0caadc32aa27a0d89da7174aed38326a879
      - EUREKA_URL=http://eureka-server:8761/eureka/
    depends_on:
      - eureka-server
      - user-service
      - workout-service
      - gamification-service
```

## 🔒 **Security Best Practices**

### 1. Secret Management

```bash
# Use environment variables for secrets
export JWT_SECRET="your-very-secure-256-bit-secret-key"
export JWT_EXPIRATION="86400000"
```

### 2. Token Validation Strategy

- **API Gateway**: Full JWT validation and user extraction
- **Downstream Services**: Trust gateway headers + validation for direct access
- **Service-to-Service**: Use service tokens or mutual TLS

### 3. Error Handling

```java
@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ACCESS_DENIED", "Insufficient permissions"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("AUTHENTICATION_FAILED", "Invalid or expired token"));
    }
}
```

## 🧪 **Testing Strategy**

### 1. Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthenticationIntegrationTest {

    @Test
    void shouldAuthenticateAndAccessProtectedEndpoint() {
        // 1. Register user
        // 2. Login and get JWT
        // 3. Access protected endpoint with JWT
        // 4. Verify response
    }
}
```

### 2. Security Tests

```java
@Test
void shouldDenyAccessWithoutToken() {
    // Test accessing protected endpoint without JWT
}

@Test
void shouldDenyAccessWithExpiredToken() {
    // Test with expired JWT
}

@Test
void shouldDenyAccessWithInvalidRole() {
    // Test admin endpoint with user token
}
```

## 📊 **Implementation Timeline**

1. **Week 1**: Shared JWT library + User Service authentication
2. **Week 2**: Workout Service JWT validation + API Gateway updates
3. **Week 3**: Gamification Service + Service-to-service security
4. **Week 4**: Testing, documentation, and deployment

This architecture provides:

- ✅ **Centralized Authentication** (User Service)
- ✅ **Distributed Authorization** (Each service validates permissions)
- ✅ **Scalable Security** (Gateway + service-level validation)
- ✅ **User Context Propagation** (Headers between services)
- ✅ **Role-based Access Control** (Fine-grained permissions)

Your API Gateway is perfectly set up for this architecture! 🎉
