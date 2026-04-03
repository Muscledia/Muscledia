# Muscledia — Gamified Fitness Platform

> A distributed, event-driven fitness tracking platform built as a 6-microservice system.  
> Transforms workout tracking into an RPG-style experience with avatar progression, gamification quests, and AI-powered workout recommendations.

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────┐
│                    API Gateway (8080)                        │
│              Route all traffic · JWT validation              │
└─────────────────────┬────────────────────────────────────────┘
                      │
        ┌─────────────┼──────────────────┐
        │             │                  │
┌───────▼──────┐ ┌────▼──────────┐ ┌────▼───────────────┐
│ User Service │ │Workout Service│ │ Gamification Service│
│   (8081)     │ │   (8082)      │ │      (8083)         │
│   MySQL      │ │   MongoDB     │ │      MongoDB        │
│   JWT Auth   │ │               │ │                     │
└──────┬───────┘ └──────┬────────┘ └──────────▲──────────┘
       │                │                      │
       │         Apache Kafka                  │
       ├──── UserRegistered ───────────────────┤
       └──── WorkoutCompleted ─────────────────┤
             PersonalRecordEvent ──────────────┘
                      │
        ┌─────────────▼──────────────────────────┐
        │         Service Discovery (8761)        │
        │              Eureka Registry            │
        │   User · Workout · Gamification · AI   │
        └─────────────┬──────────────────────────┘
                      │
              ┌───────▼─────────┐
              │   AI Service    │
              │    (8084)       │
              │ Spring WebFlux  │
              │ LLaMA 1B model  │
              └─────────────────┘
```

---

## Services

| Service | Port | Database | Purpose |
|---|---|---|---|
| `muscledia-api-gateway` | 8080 | — | Routes all client requests, JWT validation |
| `muscledia-user-service` | 8081 | MySQL | Authentication, profiles, avatars — publishes events to Kafka |
| `muscledia-workout-service` | 8082 | MongoDB | Workout logging, analytics, personal records — publishes events to Kafka |
| `gamification-service` | 8083 | MongoDB | XP, badges, quests, avatar progression — consumes Kafka events |
| `muscledia-ai-service` | 8084 | — | AI-powered personalised workout recommendations via Spring WebFlux, registered with Eureka |
| `muscledia-service-discovery` | 8761 | — | Eureka service registry — all services register here |

---

## Event-Driven Communication

Services communicate asynchronously via **Apache Kafka** rather than synchronous REST calls.

| Event | Producer | Consumer | Purpose |
|---|---|---|---|
| `UserRegistered` | User Service | Gamification Service | Initialise XP profile and avatar state on account creation |
| `WorkoutCompleted` | Workout Service | Gamification Service | Award XP, update active quests, check badge eligibility |
| `PersonalRecordEvent` | Workout Service | Gamification Service | Trigger special PR-related rewards and achievement unlocks |

This decoupling means the Gamification Service can be down without affecting workout logging — events queue in Kafka and are processed on recovery.

---

## Key Technical Decisions

### Polyglot Persistence
- **MySQL** — User accounts require ACID-compliant relational storage with strict consistency for authentication and profile data
- **MongoDB** — Workout and gamification data stored as flexible BSON documents, enabling schema-free iteration across workout types and badge definitions without migrations

### AI Service Optimisation
The AI recommendation service initially used a self-hosted LLaMA 3B model. Response times exceeded 5 minutes under load — completely unusable. Through systematic profiling, three bottlenecks were identified and resolved:

1. **Model downsizing** — Switched from 3B to 1B parameter model. Response time: 5 min → ~2 min
2. **Payload reduction** — Reduced prompt context from 2,500 to 800 tokens using focused prompting with only relevant workout history
3. **Reactive architecture** — Replaced blocking thread calls with Spring WebFlux non-blocking I/O — threads freed immediately while model inference runs

**Result: 87% improvement — 312s → 41s — sustaining 450 req/sec under load**

The AI Service registers with Eureka Service Discovery alongside all other services, enabling the API Gateway to route recommendation requests without hardcoded URLs.

### API Gateway
All client traffic enters through a single Spring Cloud Gateway instance which handles JWT validation before forwarding to downstream services — keeping auth logic centralised and out of individual services.

---

## Performance Results

| Metric | Result |
|---|---|
| Service availability | 99.99% over 400+ hours of production monitoring |
| AI response time improvement | 87% (312s → 41s) |
| Throughput under load | 450 req/sec sustained |
| Database query improvement | 85–90% via advanced indexing |
| REST API endpoints | 52 across all services |
| Infrastructure cost | $0/month (Oracle Cloud Always Free Tier) |

---

## Tech Stack

| Layer | Technologies |
|---|---|
| **Backend** | Java 21, Spring Boot 3, Spring WebFlux, Spring Security, Spring Cloud Gateway |
| **Messaging** | Apache Kafka |
| **Databases** | MySQL, MongoDB |
| **Infrastructure** | Docker, Docker Compose, Oracle Cloud Infrastructure |
| **Service Discovery** | Spring Cloud Eureka |
| **API** | REST, OpenAPI/Swagger |
| **Testing** | JUnit 5, Mockito |

---

## Prerequisites

- Java 17+
- Docker & Docker Compose
- Maven 3.6+
- Git

---

## Quick Start

### 1. Clone Repository
```bash
git clone --recursive https://github.com/Muscledia/Muscledia.git
cd Muscledia

# If already cloned without --recursive
git submodule update --init --recursive
```

### 2. Setup MongoDB Keyfile

**Windows (PowerShell):**
```powershell
.\setup-mongodb-keyfile.ps1
```

**If you encounter permission errors:**
```powershell
Remove-Item -Path .\config\mongodb -Recurse -Force
New-Item -ItemType Directory -Force -Path .\config\mongodb
$folder = Get-Item .\config\mongodb
$folder.Attributes = $folder.Attributes -band (-bnot [System.IO.FileAttributes]::ReadOnly)

$bytes = New-Object byte[] 756
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$base64 = [Convert]::ToBase64String($bytes)
$base64 | Out-File -FilePath .\config\mongodb\mongo-keyfile -Encoding ASCII -NoNewline
$rng.Dispose()

Test-Path .\config\mongodb\mongo-keyfile
```

### 3. Start All Services
```bash
docker-compose up --build -d

# Or with logs visible
docker-compose up --build
```

### 4. Verify Services
```bash
docker-compose ps
curl http://localhost:8080/actuator/health
open http://localhost:8761  # Eureka dashboard — all 6 services should appear
```

### 5. Test Authentication
```bash
# Register
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

# Login and receive JWT
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "SecurePassword123!"}'
```

---

## API Documentation

| Service | Swagger UI |
|---|---|
| User Service | http://localhost:8081/swagger-ui.html |
| Workout Service | http://localhost:8082/swagger-ui.html |
| Gamification Service | http://localhost:8083/swagger-ui.html |

### Key Endpoints

```bash
# Auth
POST /api/users/register
POST /api/users/login
GET  /api/users/me

# Workouts
GET  /api/v1/workouts
POST /api/v1/workouts
GET  /api/v1/analytics/dashboard

# Gamification
GET  /api/badges
GET  /api/quests
GET  /api/users/{id}/profile
```

---

## Database Access

```bash
# MySQL
docker exec -it muscledia-mysql mysql -u springstudent -p
# Password: springstudent

# MongoDB
docker exec -it muscledia-mongodb mongosh -u admin -p
# Password: secure_mongo_password_123
use muscledia_workouts
use gamification_db
```

---

## Environment Variables

| Variable | Description |
|---|---|
| `JWT_SECRET` | JWT signing secret |
| `MYSQL_ROOT_PASSWORD` | MySQL root password |
| `MONGO_INITDB_ROOT_PASSWORD` | MongoDB root password |

---

## Troubleshooting

```bash
# Port conflicts
netstat -tulpn | grep :8080

# Database issues
docker-compose logs mysql
docker-compose logs mongodb
docker-compose restart mysql mongodb

# Service registration — verify all 6 services appear in Eureka
open http://localhost:8761

# Health checks
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health

# Logs
docker-compose logs -f user-service
docker-compose logs -f
```

---

## Known Limitations

- **Test coverage** — Automated tests were not implemented for the Kafka event pipeline (WorkoutCompleted, PersonalRecordEvent, UserRegistered). Integration testing for event-driven flows is an identified gap and a priority for future development.
- **AI service** — The LLaMA model runs locally via Docker. On machines with limited RAM, inference times will vary from the benchmarked results.

---

## What I Learned

Building Muscledia as a solo project surfaced lessons that internship work hadn't:

- **Never assume performance** — The LLaMA 3B model was chosen without benchmarking under production constraints. The 5-minute response time only surfaced during user testing two weeks before the thesis defence. Every external dependency now gets a load test before integration.
- **POC over debate** — When the database strategy was contested, a 2-day benchmark (43ms MongoDB vs 200ms MySQL for personal record detection) resolved the disagreement faster than any argument could.
- **Kafka decoupling has real operational value** — When the Gamification Service was redeployed during testing, workout logging continued uninterrupted. Events queued and replayed cleanly on recovery. That resilience wasn't planned — it was a consequence of the architecture.
- **Test coverage for async flows is harder than for REST** — Testing Kafka consumers requires embedded brokers or Testcontainers. This was underestimated during planning and left as a gap. The next version will have integration tests for all three event types.
- **Operational cost of polyglot persistence** — Running two databases adds deployment and maintenance complexity. The performance tradeoff justified it here, but it is a real cost.

---

## Context

Built as a diploma project at Uniwersytet WSB Merito Poznań.  
Evaluated by academic reviewers and technical assessors.  

---

*Questions about implementation details or architectural decisions: ericmuganga@outlook.com*
