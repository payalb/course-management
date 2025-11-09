# BI Course Management System - Complete Documentation

## Table of Contents

1. [Application Overview](#application-overview)
2. [System Architecture](#system-architecture)
3. [Design Patterns & Principles](#design-patterns--principles)
4. [Microservices Details](#microservices-details)
5. [Project Structure](#project-structure)
6. [Technology Stack](#technology-stack)
7. [Deployment Architecture](#deployment-architecture)
8. [CI/CD Pipeline](#cicd-pipeline)
9. [API Documentation](#api-documentation)
10. [Database Schema](#database-schema)

---

## Application Overview

### Description

The **BI Course Management System** is a modern, cloud-native microservices-based platform designed for managing online courses, enrollments, and user authentication. The system follows industry best practices including CQRS (Command Query Responsibility Segregation), event-driven architecture, and containerized deployment.

### Key Features

- **User Authentication & Authorization**: JWT-based secure authentication system
- **Course Management**: Full CRUD operations for course creation and management
- **CQRS Pattern**: Separate read and write models for optimal performance
- **Event-Driven Architecture**: Real-time synchronization between services using Kafka
- **Enrollment Management**: Handle student course enrollments and tracking
- **API Gateway**: Centralized entry point with authentication and routing
- **Scalable Architecture**: Kubernetes-based deployment with auto-scaling capabilities
- **Multi-Database Support**: PostgreSQL for transactional data, MongoDB for queries
- **Caching Layer**: Redis for high-performance data access
- **Responsive UI**: React-based frontend with modern UX

### Business Use Cases

1. **Course Providers**: Create and manage online courses
2. **Students**: Browse, enroll, and access course content
3. **Administrators**: Monitor system health and manage users
4. **Analytics**: Track enrollment trends and course popularity

---

## System Architecture

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                            Internet/Users                                │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          React Frontend (UI)                             │
│                         Port: 3000 / 80 (nginx)                          │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │ HTTP/REST
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      API Gateway (Spring Cloud)                          │
│                         Port: 8080 (External)                            │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │ • JWT Authentication Filter                                      │   │
│  │ • Request Routing (Spring Cloud Gateway)                         │   │
│  │ • Rate Limiting & Circuit Breaking                               │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────┬───────────────┬───────────────┬───────────────┬───────────────────┘
      │               │               │               │
      ▼               ▼               ▼               ▼
┌──────────┐   ┌─────────────┐ ┌────────────┐ ┌─────────────┐
│   Auth   │   │   Course    │ │  Course    │ │ Enrollment  │
│ Service  │   │  Command    │ │   Query    │ │  Service    │
│          │   │  Service    │ │  Service   │ │             │
│ Port:    │   │  Port:      │ │  Port:     │ │ Port:       │
│  8080    │   │   8081      │ │   8082     │ │  8080       │
└────┬─────┘   └──────┬──────┘ └─────┬──────┘ └──────┬──────┘
     │                │               │               │
     │                │               │               │
     ▼                ▼               │               ▼
┌─────────────────────────────┐      │      ┌─────────────────┐
│      PostgreSQL 14          │      │      │   PostgreSQL    │
│   (Transactional Data)      │      │      │ (Enrollments)   │
│                             │      │      │                 │
│ • auth_schema (Users, Roles)│      │      │ • Enrollments   │
│ • course_schema (Courses)   │      │      │ • Students      │
│                             │      │      │                 │
│ Port: 5432                  │      │      │ Port: 5432      │
└─────────────────────────────┘      │      └─────────────────┘
                                     │
                                     ▼
                            ┌────────────────┐
                            │   MongoDB 7    │
                            │ (Query Model)  │
                            │                │
                            │ • Courses      │
                            │ • Aggregates   │
                            │                │
                            │ Port: 27017    │
                            └────────┬───────┘
                                     │
                                     │ Cache
                                     ▼
                            ┌────────────────┐
                            │   Redis 7      │
                            │   (Cache)      │
                            │                │
                            │ Port: 6379     │
                            └────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                      Event Streaming Layer                               │
│                                                                          │
│  ┌──────────────┐        ┌─────────────────────────────────────┐       │
│  │  Zookeeper   │◄───────┤         Apache Kafka                │       │
│  │  Port: 2181  │        │         Port: 9092                  │       │
│  └──────────────┘        │                                     │       │
│                          │  Topics:                            │       │
│                          │  • course-events                    │       │
│                          │  • enrollment-events                │       │
│                          └─────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────────────┘
```

### Architecture Patterns

#### 1. Microservices Architecture

- **Independent Services**: Each service is independently deployable
- **Database per Service**: Each service has its own database
- **API Gateway Pattern**: Single entry point for all client requests
- **Service Discovery**: Kubernetes DNS for service discovery

#### 2. CQRS (Command Query Responsibility Segregation)

```
Write Side (Commands)          Event Bus           Read Side (Queries)
┌──────────────────┐           ┌─────┐           ┌──────────────────┐
│ Course Command   │──Event──►│Kafka│──Event──►│ Course Query     │
│ Service          │           └─────┘           │ Service          │
│ (PostgreSQL)     │                             │ (MongoDB)        │
└──────────────────┘                             └──────────────────┘
     Create/Update/Delete                         Read/Search/Filter
```

**Benefits:**

- Optimized read and write models
- Independent scaling of read/write operations
- Better performance and scalability
- Eventual consistency with event sourcing

#### 3. Event-Driven Architecture

- **Event Producer**: Command Service publishes events to Kafka
- **Event Consumer**: Query Service subscribes to events
- **Decoupling**: Services communicate asynchronously
- **Resilience**: Services can work independently

#### 4. API Gateway Pattern

```
Client ──► API Gateway ──┬──► Auth Service
                         ├──► Command Service
                         ├──► Query Service
                         └──► Enrollment Service
```

**Responsibilities:**

- Authentication and Authorization
- Request Routing
- Load Balancing
- Rate Limiting
- Protocol Translation

---

## Design Patterns & Principles

### 1. Domain-Driven Design (DDD)

```
src/main/java/com/bi/
├── domain/          # Core business logic
│   ├── model/       # Domain entities
│   ├── repository/  # Repository interfaces
│   └── service/     # Domain services
├── application/     # Application services
├── infrastructure/  # Technical concerns
└── presentation/    # Controllers, DTOs
```

### 2. Clean Architecture Layers

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│    (Controllers, DTOs, Mappers)         │
├─────────────────────────────────────────┤
│         Application Layer               │
│    (Use Cases, Application Services)    │
├─────────────────────────────────────────┤
│           Domain Layer                  │
│   (Entities, Value Objects, Rules)      │
├─────────────────────────────────────────┤
│       Infrastructure Layer              │
│  (Database, Kafka, External Services)   │
└─────────────────────────────────────────┘
```

### 3. Design Patterns Used

#### Repository Pattern

```java
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByName(String name);
    Page<Course> findByStatus(Status status, Pageable pageable);
}
```

#### Mapper Pattern (MapStruct)

```java
@Mapper(componentModel = "spring")
public interface CourseMapper {
    CourseResponseDTO toDTO(Course entity);
    Course toEntity(CourseRequestDTO dto);
}
```

#### Builder Pattern

```java
Course course = Course.builder()
    .name("Spring Boot Microservices")
    .description("Learn microservices")
    .status(Status.ACTIVE)
    .build();
```

#### Strategy Pattern (Authentication)

```java
@Component
public class JwtAuthenticationStrategy implements AuthenticationStrategy {
    public Authentication authenticate(String token) {
        // JWT validation logic
    }
}
```

### 4. SOLID Principles

- **Single Responsibility**: Each class has one reason to change
- **Open/Closed**: Open for extension, closed for modification
- **Liskov Substitution**: Subtypes must be substitutable
- **Interface Segregation**: Many specific interfaces over one general
- **Dependency Inversion**: Depend on abstractions, not concretions

---

## Microservices Details

### 1. Auth Service

**Purpose**: User authentication and authorization

**Technologies**:

- Spring Boot 3.3.6
- Spring Security
- JWT (JJWT 0.12.5)
- PostgreSQL
- Flyway

**Key Features**:

- User registration and login
- JWT token generation and validation
- Password encryption (BCrypt)
- Role-based access control

**Endpoints**:

```
POST   /api/v1/auth/register  - Register new user
POST   /api/v1/auth/login     - User login
POST   /api/v1/auth/refresh   - Refresh token
GET    /api/v1/auth/me        - Get current user
```

**Database Schema**:

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### 2. Course Command Service

**Purpose**: Handle write operations for courses (Create, Update, Delete)

**Technologies**:

- Spring Boot 3.3.6
- Spring Data JPA
- PostgreSQL
- Apache Kafka (Producer)
- Flyway

**Key Features**:

- Create new courses
- Update existing courses
- Delete courses
- Publish events to Kafka

**Endpoints**:

```
POST   /api/v1/courses        - Create course
PUT    /api/v1/courses/{id}   - Update course
DELETE /api/v1/courses/{id}   - Delete course
```

**Event Publishing**:

```java
@Service
public class CourseEventPublisher {
    public void publishCourseCreated(Course course) {
        CourseCreatedEvent event = new CourseCreatedEvent(course);
        kafkaTemplate.send("course-events", event);
    }
}
```

### 3. Course Query Service

**Purpose**: Handle read operations for courses (optimized for queries)

**Technologies**:

- Spring Boot 3.3.6
- Spring Data MongoDB
- MongoDB 7
- Redis (Caching)
- Apache Kafka (Consumer)

**Key Features**:

- Search and filter courses
- Pagination and sorting
- Caching with Redis
- Event-driven synchronization

**Endpoints**:

```
GET /api/v1/courses-query           - Get all courses (paginated)
GET /api/v1/courses-query/{id}      - Get course by ID
GET /api/v1/courses-query/search    - Search courses
```

**Caching Strategy**:

```java
@Cacheable(value = "courses", key = "#id")
public CourseDTO getCourseById(Long id) {
    return courseRepository.findById(id)
        .map(courseMapper::toDTO)
        .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
}
```

### 4. Enrollment Service

**Purpose**: Manage student enrollments in courses

**Technologies**:

- Spring Boot 3.3.6
- Spring Data JPA
- PostgreSQL
- Flyway

**Key Features**:

- Enroll students in courses
- View enrollment history
- Track enrollment status
- Cancel enrollments

**Endpoints**:

```
POST   /api/v1/enrollments             - Create enrollment
GET    /api/v1/enrollments              - Get all enrollments
GET    /api/v1/enrollments/{id}         - Get enrollment by ID
DELETE /api/v1/enrollments/{id}         - Cancel enrollment
GET    /api/v1/enrollments/user/{userId} - Get user enrollments
```

### 5. API Gateway Service

**Purpose**: Entry point for all client requests

**Technologies**:

- Spring Cloud Gateway
- Spring WebFlux (Reactive)
- JWT Authentication
- Circuit Breaker

**Key Features**:

- Request routing
- JWT authentication filter
- Rate limiting
- Circuit breaker pattern
- CORS configuration

**Route Configuration**:

```java
@Bean
public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("auth-service", r -> r.path("/api/v1/auth/**")
            .uri("http://auth-service:8080"))
        .route("course-command", r -> r.path("/api/v1/courses/**")
            .and().method("POST", "PUT", "DELETE")
            .filters(f -> f.filter(authFilter))
            .uri("http://course-command-service:8081"))
        .build();
}
```

### 6. Frontend (React UI)

**Purpose**: User interface for the application

**Technologies**:

- React 18
- React Router
- Axios
- Material-UI / Tailwind CSS
- Nginx (Production)

**Key Features**:

- Course browsing and search
- User authentication
- Enrollment management
- Responsive design

---

## Project Structure

### Overall Structure

```
bi-course-management/
├── bi-parent/                    # Parent POM for all services
├── bi-parent-rdbms/              # Parent POM for RDBMS services
├── bi-auth-service/              # Authentication service
├── bi-api-gateway-service/       # API Gateway
├── bi-course-command-service/    # Course write operations
├── bi-course-query-service/      # Course read operations
├── bi-enrollment-service/        # Enrollment management
├── bi-k8s-config/                # Kubernetes configurations
└── course-react/                 # React frontend
```

### Service Structure (Example: Auth Service)

```
bi-auth-service/
├── src/
│   ├── main/
│   │   ├── java/com/bi/
│   │   │   ├── Application.java              # Main entry point
│   │   │   ├── config/                       # Configuration classes
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── JwtConfig.java
│   │   │   ├── controller/                   # REST controllers
│   │   │   │   └── AuthController.java
│   │   │   ├── service/                      # Business logic
│   │   │   │   ├── AuthService.java
│   │   │   │   └── JwtService.java
│   │   │   ├── repository/                   # Data access
│   │   │   │   └── UserRepository.java
│   │   │   ├── entity/                       # JPA entities
│   │   │   │   └── User.java
│   │   │   ├── dto/                          # Data Transfer Objects
│   │   │   │   ├── request/
│   │   │   │   │   ├── LoginRequestDTO.java
│   │   │   │   │   └── RegisterRequestDTO.java
│   │   │   │   └── response/
│   │   │   │       ├── AuthResponseDTO.java
│   │   │   │       └── UserResponseDTO.java
│   │   │   ├── mapper/                       # MapStruct mappers
│   │   │   │   └── UserMapper.java
│   │   │   ├── exception/                    # Custom exceptions
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   └── security/                     # Security components
│   │   │       ├── JwtAuthenticationFilter.java
│   │   │       └── UserDetailsServiceImpl.java
│   │   └── resources/
│   │       ├── application.yml               # Main configuration
│   │       ├── application-dev.yml           # Dev configuration
│   │       ├── application-prod.yml          # Prod configuration
│   │       └── db/migration/                 # Flyway migrations
│   │           ├── V1__Create_users_table.sql
│   │           └── V2__Add_roles.sql
│   └── test/
│       └── java/com/bi/
│           ├── controller/                   # Controller tests
│           ├── service/                      # Service tests
│           └── integration/                  # Integration tests
├── Dockerfile                                # Docker image definition
├── Jenkinsfile                               # CI/CD pipeline
├── pom.xml                                   # Maven configuration
└── README.md                                 # Service documentation
```

### Kubernetes Configuration Structure

```
bi-k8s-config/
├── base/                                     # Base configurations
│   ├── namespace.yaml
│   ├── kustomization.yaml
│   ├── configmaps/
│   │   ├── postgres-config.yaml
│   │   ├── mongodb-config.yaml
│   │   └── shared-config.yaml
│   ├── secrets/
│   │   ├── db-credentials.yaml
│   │   └── jwt-secret.yaml
│   ├── deployments/
│   │   └── infrastructure.yaml              # Databases, Kafka, Redis
│   ├── services/
│   │   ├── auth-service.yaml
│   │   ├── api-gateway-service.yaml
│   │   ├── course-command-service.yaml
│   │   ├── course-query-service.yaml
│   │   └── enrollment-service.yaml
│   └── storage/
│       └── persistent-volumes.yaml
├── overlays/
│   ├── dev/                                  # Development environment
│   │   ├── kustomization.yaml
│   │   └── deployments-patch.yaml
│   └── prod/                                 # Production environment
│       ├── kustomization.yaml
│       └── deployments-patch.yaml
├── jenkins/
│   ├── SharedPipeline.groovy               # Shared pipeline library
│   └── README.md
├── docker-compose.yml                       # Local development
├── JENKINS-SETUP.md                         # CI/CD setup guide
└── README.md                                # K8s documentation
```

---

## Technology Stack

### Backend Technologies

| Category     | Technology           | Version | Purpose                      |
| ------------ | -------------------- | ------- | ---------------------------- |
| Framework    | Spring Boot          | 3.3.6   | Application framework        |
| Language     | Java                 | 17      | Programming language         |
| Build Tool   | Maven                | 3.9+    | Dependency management        |
| API Gateway  | Spring Cloud Gateway | 4.1.3   | API routing                  |
| Security     | Spring Security      | 6.x     | Authentication/Authorization |
| JWT          | JJWT                 | 0.12.5  | Token generation             |
| ORM          | Spring Data JPA      | 3.3.x   | Data persistence             |
| Database     | PostgreSQL           | 14      | Relational database          |
| NoSQL        | MongoDB              | 7       | Document database            |
| Cache        | Redis                | 7       | In-memory cache              |
| Messaging    | Apache Kafka         | 7.5.0   | Event streaming              |
| Coordination | Zookeeper            | 3.9.x   | Kafka coordination           |
| Migration    | Flyway               | 9.22.3  | Database versioning          |
| Mapping      | MapStruct            | 1.5.5   | Object mapping               |
| Utils        | Lombok               | 1.18.30 | Boilerplate reduction        |
| API Docs     | SpringDoc OpenAPI    | 2.3.0   | API documentation            |

### Frontend Technologies

| Technology   | Version | Purpose      |
| ------------ | ------- | ------------ |
| React        | 18+     | UI framework |
| React Router | 6+      | Navigation   |
| Axios        | 1.6+    | HTTP client  |
| Nginx        | 1.25+   | Web server   |

### DevOps & Infrastructure

| Category           | Technology | Version | Purpose                 |
| ------------------ | ---------- | ------- | ----------------------- |
| Containerization   | Docker     | 24+     | Container runtime       |
| Orchestration      | Kubernetes | 1.28+   | Container orchestration |
| CI/CD              | Jenkins    | 2.4+    | Automation              |
| Version Control    | Git        | 2.x     | Source control          |
| Container Registry | Docker Hub | -       | Image storage           |

---

## Deployment Architecture

### Kubernetes Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Kubernetes Cluster                        │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Namespace: bi-course-dev                │   │
│  │                                                      │   │
│  │  ┌────────────────────────────────────────────┐     │   │
│  │  │         Ingress Controller                  │     │   │
│  │  │  (External Access - LoadBalancer)          │     │   │
│  │  └──────────────────┬──────────────────────────┘     │   │
│  │                     │                                │   │
│  │  ┌──────────────────▼──────────────────────────┐     │   │
│  │  │       API Gateway Service (2 replicas)      │     │   │
│  │  │         ClusterIP: 10.x.x.x                 │     │   │
│  │  └─────┬────────┬────────┬────────┬────────────┘     │   │
│  │        │        │        │        │                  │   │
│  │  ┌─────▼───┐ ┌──▼────┐ ┌▼─────┐ ┌▼──────────┐       │   │
│  │  │  Auth   │ │Command│ │Query │ │Enrollment │       │   │
│  │  │ Service │ │Service│ │Service│ │ Service  │       │   │
│  │  │(2 pods) │ │(2 pods)│ │(2 pods│ │(2 pods)  │       │   │
│  │  └────┬────┘ └───┬───┘ └───┬──┘ └────┬──────┘       │   │
│  │       │          │         │         │              │   │
│  │  ┌────▼──────────▼─────────┘         │              │   │
│  │  │   PostgreSQL StatefulSet          │              │   │
│  │  │   (1 pod + PVC: 1Gi)              │              │   │
│  │  └───────────────────────────────────┘              │   │
│  │                  │                                   │   │
│  │  ┌───────────────▼─────────────┐                    │   │
│  │  │   MongoDB StatefulSet       │                    │   │
│  │  │   (1 pod + PVC: 1Gi)        │                    │   │
│  │  └────────────┬────────────────┘                    │   │
│  │               │                                     │   │
│  │  ┌────────────▼──────────┐                          │   │
│  │  │   Redis Deployment     │                          │   │
│  │  │   (1 pod + PVC: 500Mi) │                          │   │
│  │  └────────────────────────┘                          │   │
│  │                                                      │   │
│  │  ┌──────────────────────────────────────────────┐   │   │
│  │  │         Kafka StatefulSet (1 pod)            │   │   │
│  │  │         + Zookeeper (1 pod)                  │   │   │
│  │  └──────────────────────────────────────────────┘   │   │
│  │                                                      │   │
│  │  ┌──────────────────────────────────────────────┐   │   │
│  │  │         Frontend Deployment (2 pods)         │   │   │
│  │  │         Nginx serving React app              │   │   │
│  │  └──────────────────────────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Resource Allocation

| Component          | Replicas | CPU Request | Memory Request | CPU Limit | Memory Limit |
| ------------------ | -------- | ----------- | -------------- | --------- | ------------ |
| API Gateway        | 2        | 250m        | 512Mi          | 500m      | 1Gi          |
| Auth Service       | 2        | 250m        | 512Mi          | 500m      | 1Gi          |
| Command Service    | 2        | 250m        | 512Mi          | 500m      | 1Gi          |
| Query Service      | 2        | 250m        | 512Mi          | 500m      | 1Gi          |
| Enrollment Service | 2        | 250m        | 512Mi          | 500m      | 1Gi          |
| PostgreSQL         | 1        | 500m        | 1Gi            | 1000m     | 2Gi          |
| MongoDB            | 1        | 500m        | 1Gi            | 1000m     | 2Gi          |
| Kafka              | 1        | 500m        | 1Gi            | 1000m     | 2Gi          |
| Redis              | 1        | 250m        | 256Mi          | 500m      | 512Mi        |
| Frontend           | 2        | 100m        | 128Mi          | 200m      | 256Mi        |

### Storage

| Component  | Storage Type | Size  | Access Mode   |
| ---------- | ------------ | ----- | ------------- |
| PostgreSQL | PVC          | 1Gi   | ReadWriteOnce |
| MongoDB    | PVC          | 1Gi   | ReadWriteOnce |
| Kafka      | PVC          | 1Gi   | ReadWriteOnce |
| Zookeeper  | PVC          | 1Gi   | ReadWriteOnce |
| Redis      | PVC          | 500Mi | ReadWriteOnce |

### Network Services

| Service            | Type      | Port  | Target Port | Purpose         |
| ------------------ | --------- | ----- | ----------- | --------------- |
| API Gateway        | ClusterIP | 80    | 8080        | External access |
| Auth Service       | ClusterIP | 80    | 8080        | Internal        |
| Command Service    | ClusterIP | 80    | 8081        | Internal        |
| Query Service      | ClusterIP | 80    | 8082        | Internal        |
| Enrollment Service | ClusterIP | 80    | 8080        | Internal        |
| PostgreSQL         | ClusterIP | 5432  | 5432        | Internal        |
| MongoDB            | ClusterIP | 27017 | 27017       | Internal        |
| Kafka              | ClusterIP | 9092  | 9092        | Internal        |
| Redis              | ClusterIP | 6379  | 6379        | Internal        |
| Frontend           | ClusterIP | 80    | 80          | External        |

---

## CI/CD Pipeline

### Pipeline Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                     Developer Workflow                        │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  Git Repository (GitHub/GitLab)                              │
│  • Feature branches                                          │
│  • Pull requests                                             │
│  • Main branch                                               │
└────────────────────────┬─────────────────────────────────────┘
                         │ Webhook
                         ▼
┌──────────────────────────────────────────────────────────────┐
│                    Jenkins CI/CD Server                       │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  Stage 1: Checkout                                     │  │
│  │  • Clone repository                                    │  │
│  │  • Get commit hash                                     │  │
│  └────────────────────────┬───────────────────────────────┘  │
│                           │                                  │
│  ┌────────────────────────▼───────────────────────────────┐  │
│  │  Stage 2: Build                                        │  │
│  │  • Maven clean package                                 │  │
│  │  • Compile Java code                                   │  │
│  └────────────────────────┬───────────────────────────────┘  │
│                           │                                  │
│  ┌────────────────────────▼───────────────────────────────┐  │
│  │  Stage 3: Test                                         │  │
│  │  • Run unit tests                                      │  │
│  │  • Generate test reports                               │  │
│  └────────────────────────┬───────────────────────────────┘  │
│                           │                                  │
│  ┌────────────────────────▼───────────────────────────────┐  │
│  │  Stage 4: Build Docker Image                           │  │
│  │  • docker build -t service:${BUILD_NUMBER}             │  │
│  └────────────────────────┬───────────────────────────────┘  │
│                           │                                  │
│  ┌────────────────────────▼───────────────────────────────┐  │
│  │  Stage 5: Push to Registry                             │  │
│  │  • docker push service:${BUILD_NUMBER}                 │  │
│  │  • docker push service:latest                          │  │
│  └────────────────────────┬───────────────────────────────┘  │
│                           │                                  │
│  ┌────────────────────────▼───────────────────────────────┐  │
│  │  Stage 6: Deploy to Kubernetes                         │  │
│  │  • kubectl set image deployment/service                │  │
│  │  • kubectl rollout status                              │  │
│  └────────────────────────┬───────────────────────────────┘  │
│                           │                                  │
│  ┌────────────────────────▼───────────────────────────────┐  │
│  │  Stage 7: Health Check                                 │  │
│  │  • Wait for pod ready                                  │  │
│  │  • Verify service health                               │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

### Jenkins Pipeline Stages

1. **Checkout**: Clone repository and get Git commit hash
2. **Build**: Compile code with Maven
3. **Test**: Run unit tests and publish reports
4. **Code Quality**: SonarQube analysis (optional)
5. **Build Docker**: Create Docker image with version tag
6. **Security Scan**: Trivy vulnerability scan (optional)
7. **Push Registry**: Push image to Docker Hub
8. **Deploy Dev**: Auto-deploy to dev environment
9. **Deploy Staging**: Auto-deploy to staging (staging branch)
10. **Deploy Prod**: Manual approval required (main branch)
11. **Health Check**: Verify deployment success

### Environment Strategy

| Environment | Branch     | Auto-Deploy | Approval Required |
| ----------- | ---------- | ----------- | ----------------- |
| Development | dev        | Yes         | No                |
| Staging     | staging    | Yes         | No                |
| Production  | main       | No          | Yes               |
| Feature     | feature/\* | Build only  | N/A               |

---

## API Documentation

### Authentication Endpoints

#### Register User

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "john.doe",
  "email": "john@example.com",
  "password": "SecurePassword123",
  "role": "STUDENT"
}

Response: 201 Created
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "id": 1,
    "username": "john.doe",
    "email": "john@example.com",
    "role": "STUDENT",
    "token": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

#### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "john.doe",
  "password": "SecurePassword123"
}

Response: 200 OK
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "type": "Bearer",
    "expiresIn": 86400
  }
}
```

### Course Management Endpoints

#### Create Course (Command)

```http
POST /api/v1/courses
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Spring Boot Microservices",
  "description": "Learn to build scalable microservices",
  "instructor": "John Doe",
  "duration": "40 hours",
  "level": "INTERMEDIATE",
  "price": 99.99
}

Response: 201 Created
{
  "success": true,
  "message": "Course created successfully",
  "data": {
    "id": 1,
    "name": "Spring Boot Microservices",
    ...
  }
}
```

#### Get All Courses (Query)

```http
GET /api/v1/courses-query?page=0&size=20&sort=name,asc
Authorization: Bearer {token}

Response: 200 OK
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

### Enrollment Endpoints

#### Create Enrollment

```http
POST /api/v1/enrollments
Authorization: Bearer {token}
Content-Type: application/json

{
  "userId": 1,
  "courseId": 1
}

Response: 201 Created
{
  "success": true,
  "message": "Enrollment created successfully",
  "data": {
    "id": 1,
    "userId": 1,
    "courseId": 1,
    "status": "ACTIVE",
    "enrolledAt": "2025-11-09T10:30:00Z"
  }
}
```

---

## Database Schema

### PostgreSQL - Auth Schema

```sql
-- Users Table
CREATE TABLE auth_schema.users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_users_username ON auth_schema.users(username);
CREATE INDEX idx_users_email ON auth_schema.users(email);
```

### PostgreSQL - Course Schema

```sql
-- Courses Table
CREATE TABLE course_schema.courses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    instructor VARCHAR(100),
    duration VARCHAR(50),
    level VARCHAR(20),
    price DECIMAL(10, 2),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_courses_name ON course_schema.courses(name);
CREATE INDEX idx_courses_status ON course_schema.courses(status);
```

### PostgreSQL - Enrollment Schema

```sql
-- Enrollments Table
CREATE TABLE enrollment_schema.enrollments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    enrolled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    UNIQUE(user_id, course_id)
);

-- Indexes
CREATE INDEX idx_enrollments_user_id ON enrollment_schema.enrollments(user_id);
CREATE INDEX idx_enrollments_course_id ON enrollment_schema.enrollments(course_id);
```

### MongoDB - Course Query Model

```javascript
{
  "_id": ObjectId("..."),
  "courseId": 1,
  "name": "Spring Boot Microservices",
  "description": "Learn to build scalable microservices",
  "instructor": "John Doe",
  "duration": "40 hours",
  "level": "INTERMEDIATE",
  "price": 99.99,
  "status": "ACTIVE",
  "enrollmentCount": 150,
  "rating": 4.5,
  "tags": ["spring", "microservices", "java"],
  "createdAt": ISODate("2025-01-01T00:00:00Z"),
  "updatedAt": ISODate("2025-11-09T00:00:00Z")
}
```

---

## Security

### Authentication Flow

```
1. User sends credentials → Auth Service
2. Auth Service validates → Generate JWT token
3. Client stores token → Include in subsequent requests
4. API Gateway validates token → Forward to services
```

### JWT Token Structure

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "john.doe",
    "role": "STUDENT",
    "iat": 1699545600,
    "exp": 1699632000
  }
}
```

### Security Best Practices

- ✅ JWT tokens with expiration
- ✅ BCrypt password hashing
- ✅ HTTPS/TLS in production
- ✅ CORS configuration
- ✅ Rate limiting
- ✅ Input validation
- ✅ SQL injection prevention (JPA)
- ✅ Secrets in Kubernetes Secrets
- ✅ Role-based access control

---

## Monitoring & Observability

### Kubernetes Monitoring

```bash
# Check pod status
kubectl get pods -n bi-course-dev

# View logs
kubectl logs -f <pod-name> -n bi-course-dev

# Describe pod
kubectl describe pod <pod-name> -n bi-course-dev

# Resource usage
kubectl top pods -n bi-course-dev
kubectl top nodes
```

### Application Metrics

- **Health Endpoints**: `/actuator/health` (if enabled)
- **Metrics**: Prometheus integration
- **Logging**: Centralized with ELK stack
- **Tracing**: Distributed tracing with Jaeger

---

## Deployment Plan

### Phase 1: Infrastructure Setup

1. Set up Kubernetes cluster
2. Create namespaces and RBAC
3. Deploy infrastructure components (PostgreSQL, MongoDB, Kafka, Redis)
4. Configure persistent storage

### Phase 2: Service Deployment

1. Deploy Auth Service
2. Deploy Course Command Service
3. Deploy Course Query Service
4. Deploy Enrollment Service
5. Deploy API Gateway

### Phase 3: Frontend Deployment

1. Build React application
2. Create Docker image
3. Deploy to Kubernetes
4. Configure ingress

### Phase 4: CI/CD Setup

1. Configure Jenkins
2. Create pipeline jobs
3. Set up webhooks
4. Test automated deployment

### Phase 5: Testing & Validation

1. Integration testing
2. Load testing
3. Security testing
4. User acceptance testing

### Phase 6: Production Release

1. Final security audit
2. Performance optimization
3. Documentation review
4. Go-live

---

## Troubleshooting Guide

### Common Issues

#### Pods Not Starting

- Check resource limits
- Verify image availability
- Check configuration errors
- Review logs

#### Database Connection Issues

- Verify credentials
- Check network connectivity
- Confirm service discovery
- Review database logs

#### Service Communication Failures

- Check service names
- Verify ports
- Test with curl from pod
- Review API Gateway routing

---

## Appendix

### Useful Commands

```bash
# Build all services
mvn clean install -DskipTests

# Build Docker images
docker build -t bi-auth-service:latest ./bi-auth-service

# Deploy to Kubernetes
kubectl apply -k overlays/dev

# Scale deployment
kubectl scale deployment/dev-auth-service --replicas=3 -n bi-course-dev

# Rollback deployment
kubectl rollout undo deployment/dev-auth-service -n bi-course-dev

# View rollout history
kubectl rollout history deployment/dev-auth-service -n bi-course-dev
```

### References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Apache Kafka](https://kafka.apache.org/documentation/)
- [PostgreSQL](https://www.postgresql.org/docs/)
- [MongoDB](https://docs.mongodb.com/)

---

**Document Version**: 1.0  
**Last Updated**: November 9, 2025  
**Maintained By**: Development Team
