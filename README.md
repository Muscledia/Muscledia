# Muscledia - Gamified Fitness Platform

A comprehensive microservices-based fitness platform that transforms workouts into gaming adventures. Users can log exercises, complete quests, evolve avatars, and battle muscle champions while tracking their fitness journey.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway (8080)                      │
│                   Route & Authenticate                     │
└─────────────────────┬───────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
┌───────▼──────┐ ┌────▼────┐ ┌──────▼──────┐
│ User Service │ │ Workout │ │Gamification │
│    (8081)    │ │ Service │ │   Service   │
│   MySQL      │ │ (8082)  │ │   (8083)    │
│   JWT Auth   │ │ MongoDB │ │   MongoDB   │
└──────────────┘ └─────────┘ └─────────────┘
        │             │             │
        └─────────────┼─────────────┘
                      │
              ┌───────▼────────┐
              │ Service        │
              │ Discovery      │
              │ (8761)         │
              └────────────────┘
```

## Services

| Service | Port | Database | Purpose |
|---------|------|----------|---------|
| **API Gateway** | 8080 | - | Route requests, JWT validation |
| **User Service** | 8081 | MySQL | Authentication, profiles, avatars |
| **Workout Service** | 8082 | MongoDB | Workout logging, analytics, routines |
| **Gamification Service** | 8083 | MongoDB | Badges, quests, achievements |
| **Service Discovery** | 8761 | - | Eureka service registry |

## Prerequisites

- **Java 17+**
- **Docker & Docker Compose**
- **Maven 3.6+**
- **Git**

## Quick Start

### 1. Clone Repository
```bash
git clone https://github.com/eric-muganga/Muscledia.git
cd Muscledia
```

### 2. Start Infrastructure
```bash
# Start all services with Docker Compose
docker-compose up --build -d

# Or start in foreground to see logs
docker-compose up --build
```

### 3. Verify Services
```bash
# Check service health
docker-compose ps

# Test API Gateway
curl http://localhost:8080/actuator/health

# View Eureka dashboard
open http://localhost:8761
```

### 4. Test Authentication
```bash
# Register a user
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com", 
    "password": "SecurePassword123!",
    "birthDate": "1990-01-01",
    "gender": "MALE",
    "height": 180,
    "initialWeight": 75,
    "goalType": "BUILD_STRENGTH",
    "initialAvatarType": "WEREWOLF"
  }'

# Login and get JWT token
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "SecurePassword123!"
  }'
```

## Development Setup

### Local Development (Without Docker)

1. **Start Dependencies**
   ```bash
   # Start only databases and infrastructure
   docker-compose up mysql mongodb zookeeper broker service-discovery -d
   ```

2. **Configure Environment**
   ```bash
   # Set environment variables for local development
   export SPRING_PROFILES_ACTIVE=local
   export MYSQL_URL=jdbc:mysql://localhost:3307/muscledia
   export MONGODB_URI=mongodb://localhost:27017/muscledia_workouts
   ```

3. **Run Services Individually**
   ```bash
   # Terminal 1: User Service
   cd services/muscledia-user-service
   mvn spring-boot:run

   # Terminal 2: Workout Service  
   cd services/muscledia-workout-service
   mvn spring-boot:run

   # Terminal 3: Gamification Service
   cd services/muscledia-gamification-service
   mvn spring-boot:run
   ```

### Building Services

```bash
# Build all services
./scripts/build-all.sh

# Build specific service
cd services/muscledia-user-service
mvn clean package -DskipTests
```

## API Documentation

### Access Swagger UI
- **User Service**: http://localhost:8081/swagger-ui.html
- **Workout Service**: http://localhost:8082/swagger-ui.html
- **Gamification Service**: http://localhost:8083/swagger-ui.html

### Key Endpoints

#### Authentication
```bash
POST /api/users/register  # Create account
POST /api/users/login     # Get JWT token
GET  /api/users/me        # Get profile
```

#### Workouts
```bash
GET  /api/v1/workouts           # List user workouts
POST /api/v1/workouts           # Log workout
GET  /api/v1/analytics/dashboard # Analytics
```

#### Gamification
```bash
GET  /api/badges               # Available badges
GET  /api/quests               # Active quests
GET  /api/users/{id}/profile   # Gamification profile
```

## Database Access

### MySQL (User Service)
```bash
# Connect to MySQL
docker exec -it muscledia-mysql mysql -u springstudent -p
# Password: springstudent
USE muscledia;
```

### MongoDB (Workout & Gamification)
```bash
# Connect to MongoDB
docker exec -it muscledia-mongodb mongosh -u admin -p
# Password: secure_mongo_password_123

# Switch databases
use muscledia_workouts      # Workout data
use gamification_db         # Gamification data
```

## Environment Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | (provided) | JWT signing secret |
| `MYSQL_ROOT_PASSWORD` | `secure_root_password_123` | MySQL root password |
| `MONGO_INITDB_ROOT_PASSWORD` | `secure_mongo_password_123` | MongoDB password |

### Profiles
- `default`: Local development with external databases
- `docker`: Containerized deployment
- `test`: Testing configuration

## Troubleshooting

### Common Issues

1. **Port Already in Use**
   ```bash
   # Check what's using the port
   netstat -tulpn | grep :8080
   
   # Kill process or change port in docker-compose.yml
   ```

2. **Database Connection Issues**
   ```bash
   # Check database containers
   docker-compose logs mysql
   docker-compose logs mongodb
   
   # Restart databases
   docker-compose restart mysql mongodb
   ```

3. **Service Registration Issues**
   ```bash
   # Check Eureka dashboard
   open http://localhost:8761
   
   # Restart service discovery
   docker-compose restart service-discovery
   ```

### Health Checks
```bash
# Check all service health
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8081/actuator/health  # User Service
curl http://localhost:8082/actuator/health  # Workout Service
curl http://localhost:8083/actuator/health  # Gamification Service
```

### Logs
```bash
# View logs for specific service
docker-compose logs -f user-service
docker-compose logs -f workout-service

# View all logs
docker-compose logs -f
```

## Development Workflow

### Making Changes

1. **Create Feature Branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make Changes**
   - Modify code in respective service directories
   - Update tests as needed
   - Test locally with Docker Compose

3. **Test Changes**
   ```bash
   # Rebuild and test
   docker-compose up --build service-name
   
   # Run integration tests
   ./scripts/test-integration.sh
   ```

4. **Commit and Push**
   ```bash
   git add .
   git commit -m "feat: add your feature description"
   git push origin feature/your-feature-name
   ```

### Code Standards
- Follow Spring Boot best practices
- Use conventional commit messages
- Add API documentation with OpenAPI/Swagger
- Include unit and integration tests
- Update README when adding new features

## Production Deployment

### Docker Production Build
```bash
# Build production images
docker-compose -f docker-compose.prod.yml build

# Deploy to production
docker-compose -f docker-compose.prod.yml up -d
```

### Environment-Specific Configurations
- Copy `application-docker.yml` to `application-prod.yml`
- Update database URLs, secrets, and resource limits
- Use external secret management for sensitive data

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Commit your changes (`git commit -m 'Add amazing feature'`)
7. Push to the branch (`git push origin feature/amazing-feature`)
8. Open a Pull Request

## Support

For questions or issues:
- Check existing [GitHub Issues](https://github.com/eric-muganga/Muscledia/issues)
- Create a new issue with detailed description
- Contact the development team

---

**Tech Stack**: Spring Boot, Docker, MySQL, MongoDB, Kafka, Eureka, JWT, React Native (planned)
**Version**: 1.0.0
**License**: MIT
