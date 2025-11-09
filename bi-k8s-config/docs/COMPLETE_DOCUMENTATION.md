# Course Management Platform - Complete Documentation

## Table of Contents
1. [Application Overview](#application-overview)
2. [System Architecture](#system-architecture)
3. [Design Patterns](#design-patterns)
4. [Service Details](#service-details)
5. [Database Design](#database-design)
6. [Deployment Architecture](#deployment-architecture)
7. [Project Structure](#project-structure)
8. [CI/CD Pipeline](#cicd-pipeline)
9. [Security](#security)
10. [Monitoring & Observability](#monitoring--observability)

---

## 1. Application Overview

### 1.1 Purpose
The Course Management Platform is a cloud-native, microservices-based application designed to manage online courses, user authentication, and student enrollments. The platform follows modern architectural patterns including CQRS, Event-Driven Architecture, and API Gateway pattern.

### 1.2 Key Features
- **User Authentication & Authorization**: JWT-based secure authentication
- **Course Management**: Create, update, and manage courses
- **Student Enrollment**: Enroll students in courses
- **Real-time Updates**: Event-driven architecture for data synchronization
- **Scalable Architecture**: Independent microservices that can scale horizontally
- **RESTful APIs**: Standard REST APIs for all operations

### 1.3 Technology Stack

#### Backend Technologies
- **Framework**: Spring Boot 3.3.6
- **Language**: Java 17
- **Build Tool**: Maven
- **Database**: 
  - PostgreSQL 14 (Relational data)
  - MongoDB 6 (Document store)
  - Redis 7 (Caching)
- **Messaging**: Apache Kafka 7.5.0
- **API Gateway**: Spring Cloud Gateway 4.1.3
- **Authentication**: JWT (JJWT 0.12.5)
- **Database Migration**: Flyway 9.22.3

#### Infrastructure
- **Container Platform**: Docker
- **Orchestration**: Kubernetes
- **CI/CD**: Jenkins
- **Version Control**: Git

#### Architecture Patterns
- Microservices Architecture
- CQRS (Command Query Responsibility Segregation)
- Event-Driven Architecture
- API Gateway Pattern
- Database per Service Pattern

---

## 2. System Architecture

### 2.1 High-Level Architecture

```
                                    ┌─────────────────┐
                                    │   React UI      │
                                    │   (Frontend)    │
                                    └────────┬────────┘
                                             │
                                             │ HTTP/REST
                                             ▼
                                    ┌─────────────────┐
                                    │   API Gateway   │
                                    │   (Port 8080)   │
                                    └────────┬────────┘
                                             │
                    ┌────────────────────────┼────────────────────────┐
                    │                        │                        │
                    ▼                        ▼                        ▼
           ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
           │  Auth Service   │     │ Course Command  │     │   Enrollment    │
           │  (Port 8080)    │     │   Service       │     │    Service      │
           │                 │     │  (Port 8081)    │     │  (Port 8080)    │
           └────────┬────────┘     └────────┬────────┘     └────────┬────────┘
                    │                       │                        │
                    ▼                       │                        ▼
           ┌─────────────────┐             │               ┌─────────────────┐
           │   PostgreSQL    │             │               │   PostgreSQL    │
           │  (Auth Schema)  │             │               │ (Enrollment DB) │
           └─────────────────┘             │               └─────────────────┘
                                            │
                                            ▼
                                   ┌─────────────────┐
                                   │   PostgreSQL    │
                                   │  (Course DB)    │
                                   └─────────────────┘
                                            │
                                            │ Events
                                            ▼
                                   ┌─────────────────┐
                                   │  Apache Kafka   │
                                   │   (Message Bus) │
                                   └────────┬────────┘
                                            │
                                            │ Subscribe
                                            ▼
                                   ┌─────────────────┐
                                   │ Course Query    │
                                   │   Service       │
                                   │  (Port 8082)    │
                                   └────────┬────────┘
                                            │
                            ┌───────────────┼───────────────┐
                            ▼                               ▼
                   ┌─────────────────┐           ┌─────────────────┐
                   │    MongoDB      │           │      Redis      │
                   │  (Read Model)   │           │     (Cache)     │
                   └─────────────────┘           └─────────────────┘
```

### 2.2 Microservices Overview

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| API Gateway | 8080 | None | Routes requests, handles authentication |
| Auth Service | 8080 | PostgreSQL | User authentication and JWT management |
| Course Command Service | 8081 | PostgreSQL | Handles write operations for courses (CQRS) |
| Course Query Service | 8082 | MongoDB, Redis | Handles read operations for courses (CQRS) |
| Enrollment Service | 8080 | PostgreSQL | Manages student enrollments |

### 2.3 Communication Patterns

#### Synchronous Communication
- **Client → API Gateway → Microservices**: REST/HTTP
- **Protocol**: JSON over HTTP
- **Authentication**: JWT Bearer Token

#### Asynchronous Communication
- **Service → Kafka → Service**: Event-driven
- **Events**: Course Created, Updated, Deleted
- **Pattern**: Publish-Subscribe

---

## 3. Design Patterns

### 3.1 CQRS (Command Query Responsibility Segregation)

**Implementation:**
- **Command Side**: Course Command Service (writes to PostgreSQL)
- **Query Side**: Course Query Service (reads from MongoDB)
- **Synchronization**: Kafka events

**Benefits:**
- Optimized read and write models
- Better scalability
- Independent scaling of read/write operations

### 3.2 Event-Driven Architecture

**Event Flow:**
```
Course Command Service → Kafka Topic → Course Query Service
                              ↓
                        Event: CourseCreated
                        Event: CourseUpdated
                        Event: CourseDeleted
```

**Event Types:**
- `course.created`: Published when a new course is created
- `course.updated`: Published when a course is modified
- `course.deleted`: Published when a course is removed

### 3.3 API Gateway Pattern

**Responsibilities:**
- Single entry point for all clients
- Request routing to appropriate microservices
- JWT token validation
- Cross-cutting concerns (logging, rate limiting)

**Routes:**
```
/api/v1/auth/**           → Auth Service
/api/v1/courses/**        → Course Command Service (POST, PUT, DELETE)
/api/v1/courses-query/**  → Course Query Service (GET)
/api/v1/enrollments/**    → Enrollment Service
```

### 3.4 Database per Service Pattern

Each microservice has its own database:
- **Auth Service**: PostgreSQL (auth_schema)
- **Course Command**: PostgreSQL (course_db)
- **Course Query**: MongoDB (course_query_db)
- **Enrollment**: PostgreSQL (enrollment_db)

**Benefits:**
- Service independence
- Technology diversity
- Easier scaling

---

## 4. Service Details

### 4.1 API Gateway Service

**Technology:** Spring Cloud Gateway (Reactive)

**Key Features:**
- Route-based request forwarding
- JWT authentication filter
- Public endpoint whitelist
- Load balancing

**Configuration:**
```yaml
Routes:
  - Auth Service: /api/v1/auth/** (Public)
  - Course Write: /api/v1/courses/** (Authenticated)
  - Course Read: /api/v1/courses-query/** (Authenticated)
  - Enrollment: /api/v1/enrollments/** (Authenticated)
```

**Security:**
- JWT token validation
- Public paths: `/api/v1/auth/login`, `/api/v1/auth/register`
- All other paths require valid JWT token

### 4.2 Auth Service

**Port:** 8080  
**Database:** PostgreSQL (auth_schema)

**Responsibilities:**
- User registration
- User login
- JWT token generation
- Password encryption (BCrypt)

**API Endpoints:**
```
POST /api/v1/auth/register - Register new user
POST /api/v1/auth/login    - Login and get JWT token
```

**Database Schema:**
```sql
Table: users
- id (BIGINT, PK)
- username (VARCHAR, UNIQUE)
- email (VARCHAR, UNIQUE)
- password (VARCHAR, BCrypt hashed)
- role (VARCHAR)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)
```

**Security:**
- Passwords hashed with BCrypt
- JWT tokens with configurable expiration
- Role-based access control

### 4.3 Course Command Service

**Port:** 8081  
**Database:** PostgreSQL (course_db)  
**Message Queue:** Kafka

**Responsibilities:**
- Create courses (Write)
- Update courses (Write)
- Delete courses (Write)
- Publish events to Kafka

**API Endpoints:**
```
POST   /api/v1/courses        - Create new course
PUT    /api/v1/courses/{id}   - Update course
DELETE /api/v1/courses/{id}   - Delete course
```

**Database Schema:**
```sql
Table: courses
- id (BIGINT, PK)
- title (VARCHAR)
- description (TEXT)
- instructor (VARCHAR)
- duration (INTEGER)
- status (VARCHAR)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)
```

**Event Publishing:**
- Publishes to Kafka topic: `course-events`
- Event types: CREATED, UPDATED, DELETED

### 4.4 Course Query Service

**Port:** 8082  
**Database:** MongoDB (course_query_db)  
**Cache:** Redis  
**Message Queue:** Kafka (Consumer)

**Responsibilities:**
- Read course data (Read)
- Cache frequently accessed data
- Listen to Kafka events
- Update read model

**API Endpoints:**
```
GET /api/v1/courses-query          - Get all courses (paginated)
GET /api/v1/courses-query/{id}     - Get course by ID
GET /api/v1/courses-query/search   - Search courses
```

**MongoDB Document Structure:**
```json
{
  "_id": "ObjectId",
  "courseId": "Long",
  "title": "String",
  "description": "String",
  "instructor": "String",
  "duration": "Integer",
  "status": "String",
  "createdAt": "Date",
  "updatedAt": "Date"
}
```

**Caching Strategy:**
- Cache individual courses in Redis
- TTL: 1 hour
- Cache invalidation on update events

### 4.5 Enrollment Service

**Port:** 8080  
**Database:** PostgreSQL (enrollment_db)

**Responsibilities:**
- Enroll students in courses
- Manage enrollment status
- Track enrollment history

**API Endpoints:**
```
POST   /api/v1/enrollments              - Create enrollment
GET    /api/v1/enrollments/{id}         - Get enrollment by ID
GET    /api/v1/enrollments/student/{id} - Get enrollments by student
DELETE /api/v1/enrollments/{id}         - Cancel enrollment
```

**Database Schema:**
```sql
Table: enrollments
- id (BIGINT, PK)
- student_id (BIGINT, FK)
- course_id (BIGINT, FK)
- enrollment_date (TIMESTAMP)
- status (VARCHAR)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)
```

---

## 5. Database Design

### 5.1 PostgreSQL Databases

#### Auth Schema
```
users
├── id (BIGSERIAL PRIMARY KEY)
├── username (VARCHAR(100) UNIQUE NOT NULL)
├── email (VARCHAR(255) UNIQUE NOT NULL)
├── password (VARCHAR(255) NOT NULL)
├── role (VARCHAR(50) NOT NULL)
├── created_at (TIMESTAMP NOT NULL)
└── updated_at (TIMESTAMP NOT NULL)

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
```

#### Course Database
```
courses
├── id (BIGSERIAL PRIMARY KEY)
├── title (VARCHAR(200) NOT NULL)
├── description (TEXT)
├── instructor (VARCHAR(100) NOT NULL)
├── duration (INTEGER)
├── status (VARCHAR(50) NOT NULL)
├── created_at (TIMESTAMP NOT NULL)
└── updated_at (TIMESTAMP NOT NULL)

CREATE INDEX idx_courses_title ON courses(title);
CREATE INDEX idx_courses_status ON courses(status);
```

#### Enrollment Database
```
enrollments
├── id (BIGSERIAL PRIMARY KEY)
├── student_id (BIGINT NOT NULL)
├── course_id (BIGINT NOT NULL)
├── enrollment_date (TIMESTAMP NOT NULL)
├── status (VARCHAR(50) NOT NULL)
├── created_at (TIMESTAMP NOT NULL)
└── updated_at (TIMESTAMP NOT NULL)

CREATE INDEX idx_enrollments_student ON enrollments(student_id);
CREATE INDEX idx_enrollments_course ON enrollments(course_id);
CREATE UNIQUE INDEX idx_enrollments_unique ON enrollments(student_id, course_id);
```

### 5.2 MongoDB Collections

#### Course Query Collection
```javascript
{
  _id: ObjectId,
  courseId: Long,
  title: String (indexed),
  description: String,
  instructor: String,
  duration: Number,
  status: String (indexed),
  createdAt: ISODate,
  updatedAt: ISODate
}

// Indexes
db.courses.createIndex({ courseId: 1 }, { unique: true })
db.courses.createIndex({ title: "text" })
db.courses.createIndex({ status: 1 })
```

### 5.3 Redis Cache Structure

```
Key Pattern: course:{courseId}
Value: JSON string of course object
TTL: 3600 seconds (1 hour)

Example:
Key: "course:123"
Value: '{"id":123,"title":"Java Programming","instructor":"John Doe",...}'
```

---

## 6. Deployment Architecture

### 6.1 Kubernetes Architecture

```
Namespace: bi-course-dev

┌─────────────────────────────────────────────────────────────────┐
│                         Ingress                                  │
│                    (External Access)                             │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  API Gateway    │
                    │  Service (LB)   │
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│Auth Service  │    │Course Cmd    │    │Enrollment    │
│Deployment    │    │Deployment    │    │Deployment    │
│(2 replicas)  │    │(2 replicas)  │    │(2 replicas)  │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                    │
       ▼                   ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│PostgreSQL    │    │PostgreSQL    │    │PostgreSQL    │
│StatefulSet   │    │StatefulSet   │    │StatefulSet   │
└──────────────┘    └──────────────┘    └──────────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  Kafka          │
                    │  StatefulSet    │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │Course Query     │
                    │Deployment       │
                    │(2 replicas)     │
                    └────────┬────────┘
                             │
                    ┌────────┴────────┐
                    ▼                 ▼
           ┌─────────────┐   ┌─────────────┐
           │  MongoDB    │   │   Redis     │
           │ StatefulSet │   │ Deployment  │
           └─────────────┘   └─────────────┘
```

### 6.2 Kubernetes Resources

#### Deployments (Application Services)
```yaml
Services:
  - dev-auth-service (replicas: 2)
  - dev-api-gateway-service (replicas: 2)
  - dev-course-command-service (replicas: 2)
  - dev-course-query-service (replicas: 2)
  - dev-enrollment-service (replicas: 2)
  - dev-course-ui (replicas: 2)
  - dev-redis (replicas: 1)

Resource Limits:
  CPU Request: 250m
  CPU Limit: 500m
  Memory Request: 512Mi
  Memory Limit: 1Gi
```

#### StatefulSets (Stateful Services)
```yaml
StatefulSets:
  - dev-postgres-0
  - dev-mongodb-0
  - dev-kafka-0
  - dev-zookeeper-0

Storage:
  PostgreSQL: 1Gi PVC
  MongoDB: 1Gi PVC
  Kafka: 1Gi PVC
  Zookeeper: 2Gi PVC (data + log)
```

#### Services
```yaml
ClusterIP Services:
  - dev-auth-service (port 80 → 8080)
  - dev-api-gateway-service (port 80 → 8080)
  - dev-course-command-service (port 80 → 8081)
  - dev-course-query-service (port 80 → 8082)
  - dev-enrollment-service (port 80 → 8080)
  - dev-postgres (port 5432)
  - dev-mongodb (port 27017)
  - dev-kafka (port 9092)
  - dev-redis (port 6379)
  - dev-zookeeper (port 2181)
```

#### ConfigMaps
```yaml
ConfigMaps:
  - dev-shared-config
  - dev-postgres-config
  - dev-mongodb-config
  - dev-kafka-config
  - dev-redis-config
  - dev-spring-config
```

#### Secrets
```yaml
Secrets:
  - dev-db-credentials
  - dev-jwt-secret
  - dev-mongodb-credentials
  - dev-postgres-credentials
```

### 6.3 Docker Compose (Local Development)

```yaml
Services:
  - postgres (PostgreSQL 14)
  - mongodb (MongoDB 6)
  - redis (Redis 7)
  - zookeeper (Confluent 7.5.0)
  - kafka (Confluent 7.5.0)
  - auth-service
  - api-gateway-service
  - course-command-service
  - course-query-service
  - enrollment-service
  - course-react (Frontend)

Networks:
  - bi-network (bridge)

Volumes:
  - postgres_data
  - mongodb_data
```

---

## 7. Project Structure

### 7.1 Repository Overview

```
bi-microservices/
├── bi-parent/                      # Parent POM
├── bi-parent-rdbms/                # RDBMS Parent POM
├── bi-auth-service/                # Authentication Service
├── bi-api-gateway/                 # API Gateway (Old)
├── bi-api-gateway-service/         # API Gateway Service
├── bi-course-command-service/      # Course Write Service
├── bi-course-query-service/        # Course Read Service
├── bi-enrollment-service/          # Enrollment Service
└── bi-k8s-config/                  # Kubernetes Configuration
```

### 7.2 Service Structure (Standard Spring Boot)

```
bi-auth-service/
├── pom.xml
├── Dockerfile
├── Jenkinsfile
├── README.md
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/bi/
│   │   │       ├── Application.java          # Main class
│   │   │       ├── config/                   # Configuration classes
│   │   │       ├── controller/               # REST controllers
│   │   │       ├── service/                  # Business logic
│   │   │       ├── repository/               # Data access
│   │   │       ├── entity/                   # JPA entities
│   │   │       ├── dto/                      # Data Transfer Objects
│   │   │       │   ├── request/              # Request DTOs
│   │   │       │   └── response/             # Response DTOs
│   │   │       ├── exception/                # Custom exceptions
│   │   │       ├── mapper/                   # MapStruct mappers
│   │   │       ├── security/                 # Security components
│   │   │       └── util/                     # Utility classes
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-docker.properties
│   │       └── db/migration/                 # Flyway migrations
│   │           ├── V1__Create_users_table.sql
│   │           └── V2__Add_roles.sql
│   └── test/
│       └── java/com/bi/
│           ├── controller/                   # Controller tests
│           ├── service/                      # Service tests
│           └── integration/                  # Integration tests
└── target/                                   # Build output
```

### 7.3 Kubernetes Configuration Structure

```
bi-k8s-config/
├── docker-compose.yml              # Local development
├── kustomization.yaml              # Base kustomization
├── JENKINS-SETUP.md               # Jenkins setup guide
├── base/                           # Base configurations
│   ├── kustomization.yaml
│   ├── namespace.yaml
│   ├── configmaps/
│   │   ├── shared-config.yaml
│   │   ├── postgres-config.yaml
│   │   ├── mongodb-config.yaml
│   │   ├── kafka-config.yaml
│   │   └── redis-config.yaml
│   ├── secrets/
│   │   ├── db-credentials.yaml
│   │   ├── jwt-secret.yaml
│   │   └── mongodb-credentials.yaml
│   ├── services/
│   │   ├── auth-service.yaml
│   │   ├── api-gateway-service.yaml
│   │   ├── course-command-service.yaml
│   │   ├── course-query-service.yaml
│   │   └── enrollment-service.yaml
│   ├── deployments/
│   │   └── infrastructure.yaml
│   ├── storage/
│   │   └── persistent-volumes.yaml
│   └── ingress/
│       └── ingress.yaml
├── overlays/                       # Environment-specific
│   ├── dev/
│   │   ├── kustomization.yaml
│   │   └── deployments-patch.yaml
│   ├── staging/
│   │   ├── kustomization.yaml
│   │   └── deployments-patch.yaml
│   └── prod/
│       ├── kustomization.yaml
│       └── deployments-patch.yaml
└── jenkins/                        # CI/CD configurations
    ├── SharedPipeline.groovy
    └── README.md
```

---

## 8. CI/CD Pipeline

### 8.1 Jenkins Pipeline Overview

```
┌──────────────┐
│   Checkout   │  Clone repository
└──────┬───────┘
       │
┌──────▼───────┐
│    Build     │  Maven package
└──────┬───────┘
       │
┌──────▼───────┐
│     Test     │  Unit & Integration tests
└──────┬───────┘
       │
┌──────▼───────┐
│Build Docker  │  Create container image
└──────┬───────┘
       │
┌──────▼───────┐
│Push Registry │  Push to Docker Hub
└──────┬───────┘
       │
┌──────▼───────┐
│  Deploy K8s  │  Update Kubernetes
└──────┬───────┘
       │
┌──────▼───────┐
│Health Check  │  Verify deployment
└──────────────┘
```

### 8.2 Pipeline Stages

**1. Checkout**
- Clone Git repository
- Get commit hash for tagging

**2. Build**
```bash
mvn clean package -DskipTests
```

**3. Test**
```bash
mvn test
```
- JUnit test reports
- Code coverage

**4. Build Docker Image**
```bash
docker build -t ${IMAGE_NAME}:${BUILD_NUMBER} .
docker tag ${IMAGE_NAME}:${BUILD_NUMBER} ${IMAGE_NAME}:latest
```

**5. Push to Registry**
```bash
docker push ${IMAGE_NAME}:${BUILD_NUMBER}
docker push ${IMAGE_NAME}:latest
```

**6. Deploy to Kubernetes**
```bash
kubectl set image deployment/${DEPLOYMENT_NAME} \
    ${CONTAINER_NAME}=${IMAGE_NAME}:${BUILD_NUMBER} \
    -n ${NAMESPACE}

kubectl rollout status deployment/${DEPLOYMENT_NAME} -n ${NAMESPACE}
```

**7. Health Check**
```bash
kubectl wait --for=condition=ready pod \
    -l app=${APP_NAME} \
    -n ${NAMESPACE} \
    --timeout=300s
```

### 8.3 Branch Strategy

| Branch | Environment | Deployment |
|--------|-------------|------------|
| feature/* | None | No auto-deploy |
| dev | Development | Automatic |
| staging | Staging | Automatic |
| main | Production | Manual approval required |

### 8.4 Jenkinsfile Locations

```
Jenkins Pipelines:
├── bi-parent/Jenkinsfile
├── bi-parent-rdbms/Jenkinsfile
├── bi-auth-service/Jenkinsfile
├── bi-api-gateway-service/Jenkinsfile
├── bi-course-command-service/Jenkinsfile
├── bi-course-query-service/Jenkinsfile
└── bi-enrollment-service/Jenkinsfile
```

---

## 9. Security

### 9.1 Authentication Flow

```
1. User Login
   ↓
2. Auth Service validates credentials
   ↓
3. Generate JWT token
   ↓
4. Return token to client
   ↓
5. Client includes token in requests
   ↓
6. API Gateway validates JWT
   ↓
7. Forward to microservice if valid
```

### 9.2 JWT Structure

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "username",
    "iat": 1699564800,
    "exp": 1699651200,
    "roles": ["USER"]
  },
  "signature": "HMACSHA256(...)"
}
```

### 9.3 Security Features

- **Password Hashing**: BCrypt with salt
- **JWT Tokens**: HMAC SHA256 signed
- **Token Expiration**: Configurable (default 24 hours)
- **HTTPS**: Required in production
- **CORS**: Configured for frontend origin
- **Input Validation**: Bean Validation on all DTOs
- **SQL Injection Prevention**: Parameterized queries via JPA

### 9.4 Kubernetes Security

```yaml
Security Measures:
  - Secrets for sensitive data
  - Service accounts with RBAC
  - Network policies
  - Resource limits
  - Read-only root filesystem (future)
  - Non-root user (future)
```

---

## 10. Monitoring & Observability

### 10.1 Logging Strategy

**Application Logs:**
```java
@Slf4j
public class UserService {
    public User createUser(UserDTO dto) {
        log.info("Creating user: {}", dto.getUsername());
        // ... business logic
        log.debug("User created with ID: {}", user.getId());
    }
}
```

**Log Levels:**
- `ERROR`: System errors
- `WARN`: Potential issues
- `INFO`: Business events (create, update, delete)
- `DEBUG`: Detailed information for debugging

**Kubernetes Logs:**
```bash
# View logs
kubectl logs -f deployment/dev-auth-service -n bi-course-dev

# View previous container logs
kubectl logs deployment/dev-auth-service -n bi-course-dev --previous
```

### 10.2 Health Checks (Future Implementation)

**Spring Boot Actuator Endpoints:**
```yaml
/actuator/health       # Application health
/actuator/health/liveness   # Kubernetes liveness
/actuator/health/readiness  # Kubernetes readiness
/actuator/metrics      # Application metrics
/actuator/prometheus   # Prometheus metrics
```

### 10.3 Monitoring Tools (Recommended)

**Metrics:**
- Prometheus: Metric collection
- Grafana: Visualization

**Logging:**
- ELK Stack (Elasticsearch, Logstash, Kibana)
- Fluentd for log aggregation

**Tracing:**
- Zipkin or Jaeger for distributed tracing
- Spring Cloud Sleuth for trace context

### 10.4 Key Metrics to Monitor

**Application Metrics:**
- Request rate
- Error rate
- Response time (latency)
- Active threads
- JVM memory usage
- Database connection pool

**Infrastructure Metrics:**
- CPU usage
- Memory usage
- Disk I/O
- Network I/O
- Pod restart count

**Business Metrics:**
- User registrations
- Course creations
- Enrollments
- API usage by endpoint

---

## Appendices

### A. API Endpoint Reference

#### Auth Service
```
POST /api/v1/auth/register
POST /api/v1/auth/login
```

#### Course Command Service
```
POST   /api/v1/courses
PUT    /api/v1/courses/{id}
DELETE /api/v1/courses/{id}
```

#### Course Query Service
```
GET /api/v1/courses-query
GET /api/v1/courses-query/{id}
GET /api/v1/courses-query/search?keyword={keyword}
```

#### Enrollment Service
```
POST   /api/v1/enrollments
GET    /api/v1/enrollments/{id}
GET    /api/v1/enrollments/student/{studentId}
DELETE /api/v1/enrollments/{id}
```

### B. Environment Variables

#### Common Variables
```
SPRING_PROFILES_ACTIVE=dev|staging|prod
SERVER_PORT=8080
```

#### Database Variables
```
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/db
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=pass
```

#### MongoDB Variables
```
SPRING_DATA_MONGODB_HOST=localhost
SPRING_DATA_MONGODB_PORT=27017
SPRING_DATA_MONGODB_DATABASE=course_query_db
SPRING_DATA_MONGODB_USERNAME=mongouser
SPRING_DATA_MONGODB_PASSWORD=mongopass
```

#### Kafka Variables
```
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

#### JWT Variables
```
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400000
```

### C. Build Commands

```bash
# Build all services
mvn clean package -DskipTests

# Build with tests
mvn clean package

# Build Docker image
docker build -t service-name:tag .

# Deploy to Kubernetes
kubectl apply -k overlays/dev

# Rollout restart
kubectl rollout restart deployment/service-name -n namespace
```

### D. Troubleshooting Guide

**Common Issues:**

1. **Service Not Starting**
   - Check logs: `kubectl logs pod-name -n namespace`
   - Check events: `kubectl describe pod pod-name -n namespace`
   - Verify environment variables and secrets

2. **Database Connection Failed**
   - Verify database is running
   - Check connection string
   - Verify credentials
   - Check network policies

3. **Kafka Connection Issues**
   - Verify Kafka is running
   - Check bootstrap servers configuration
   - Verify topic exists

4. **JWT Authentication Fails**
   - Verify JWT secret matches across services
   - Check token expiration
   - Verify token format

---

## Document Information

**Version:** 1.0  
**Last Updated:** November 9, 2025  
**Author:** Development Team  
**Status:** Final

---

**End of Documentation**
