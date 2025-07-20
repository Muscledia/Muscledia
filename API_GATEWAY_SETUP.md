# Muscledia API Gateway Setup

## Overview

This API Gateway serves as the central entry point for all Muscledia microservices, providing JWT authentication, request routing, and service discovery.

## Features

- **JWT Authentication & Authorization**: Validates tokens and enforces role-based access control
- **Request Routing**: Routes requests to appropriate microservices based on path patterns
- **Service Discovery**: Uses Eureka for dynamic service discovery
- **CORS Support**: Configured for cross-origin requests
- **User Context Forwarding**: Adds user information headers for downstream services

## Architecture

```
Client → API Gateway (Port 8080) → Microservices
                ↓
        [JWT Validation]
                ↓
        [Service Discovery via Eureka]
                ↓
        [Route to Backend Services]
```

## Supported Services

### 1. User Service (`muscledia-user-service`)

- **Authentication**: `/api/v1/auth/**`
- **User Management**: `/api/v1/users/**`

### 2. Workout Service (`muscledia-workout-service`)

- **Admin Data Population**: `/api/admin/data/**` (Admin only)
- **Exercises**: `/api/v1/exercises/**`
- **Muscle Groups**: `/api/v1/muscle-groups/**`
- **Routine Folders**: `/api/v1/routine-folders/**`
- **Workout Plans**: `/api/v1/workout-plans/**`
- **Workouts**: `/api/v1/workouts/**`
- **Analytics**: `/api/v1/analytics/**`

### 3. Gamification Service (`gamification-service`)

- **Badges**: `/api/badges/**`
- **Champions**: `/api/champions/**`
- **Quests**: `/api/quests/**`
- **User Gamification**: `/api/users/{userId}/profile`, `/api/users/{userId}/streaks/**`, etc.

## Authentication & Authorization

### Public Endpoints (No Authentication Required)

- `GET /api/v1/exercises/**` - Browse exercises
- `GET /api/v1/muscle-groups/**` - Browse muscle groups
- `GET /api/v1/workout-plans/public/**` - Public workout plans
- `GET /api/v1/routine-folders/public/**` - Public routine folders
- `/api/v1/auth/**` - Authentication endpoints
- `/gateway/**` - Gateway info endpoints

### Protected Endpoints (Authentication Required)

- `/api/v1/workouts/**` - Personal workouts
- `/api/v1/analytics/**` - Workout analytics
- `/api/badges/**` - Badge management
- `/api/champions/**` - Champions
- `/api/quests/**` - Quest system
- User-specific gamification endpoints

### Admin Only Endpoints

- `/api/admin/**` - Data population and admin functions
- `POST/PUT/DELETE /api/v1/exercises/**` - Exercise management
- `POST/PUT/DELETE /api/v1/muscle-groups/**` - Muscle group management

## Configuration

### JWT Settings

```yaml
application:
  security:
    jwt:
      secret-key: ${JWT_SECRET} # Use environment variable in production
      expiration: 86400000 # 24 hours
      header: Authorization
      prefix: "Bearer "
```

### Eureka Configuration

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
```

## Request Flow

1. **Client Request** → API Gateway (Port 8080)
2. **Authentication Check**:
   - Public endpoints: Skip JWT validation
   - Protected endpoints: Validate JWT token
   - Extract user information from token
3. **Authorization Check**:
   - Verify user has required role/permissions
4. **Service Discovery**:
   - Resolve target service via Eureka
5. **Request Forwarding**:
   - Add user context headers (`X-User-Id`, `X-Username`, `X-User-Role`)
   - Forward to appropriate microservice
6. **Response** → Client

## User Context Headers

The gateway automatically adds these headers to downstream requests:

- `X-User-Id`: User's unique identifier
- `X-Username`: User's username
- `X-User-Role`: User's role (USER, ADMIN, etc.)
- `X-User-Info`: Combined user information

## Gateway Management Endpoints

- `GET /gateway/health` - Gateway health check
- `GET /gateway/info` - Gateway information and features
- `GET /gateway/routes` - Available routes configuration

## Development Setup

1. **Start Eureka Server** (Port 8761)
2. **Start Backend Services**:
   - muscledia-user-service
   - muscledia-workout-service
   - gamification-service
3. **Start API Gateway** (Port 8080)

## Environment Variables

```bash
JWT_SECRET=your-256-bit-secret-key
JWT_EXPIRATION=86400000
EUREKA_URL=http://localhost:8761/eureka/
```

## Testing

```bash
# Health check
curl http://localhost:8080/gateway/health

# Public endpoint (no auth)
curl http://localhost:8080/api/v1/exercises

# Protected endpoint (requires JWT)
curl -H "Authorization: Bearer <jwt-token>" \
     http://localhost:8080/api/v1/workouts

# Admin endpoint (requires ADMIN role)
curl -H "Authorization: Bearer <admin-jwt-token>" \
     http://localhost:8080/api/admin/data/populate-all
```

## Security Features

- **JWT Token Validation**: Ensures only authenticated users access protected resources
- **Role-Based Access Control**: Enforces ADMIN permissions for administrative functions
- **CORS Protection**: Configured for secure cross-origin requests
- **Request Path Validation**: Routes validated against predefined patterns
- **User Context Injection**: Automatic user information forwarding to services

This setup provides a robust, scalable API Gateway that handles authentication, authorization, and intelligent request routing for the entire Muscledia ecosystem.
