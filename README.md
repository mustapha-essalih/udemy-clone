# Course Studio — Production-Grade LMS Platform

A full-stack, event-driven microservices platform for online learning — inspired by Udemy. Built with Java Spring Boot, React, PostgreSQL, Redis, Elasticsearch, Kafka, and Stripe.

## Roles

| Role | Capabilities |
|------|-------------|
| **Student** | Browse/search courses, enroll, watch videos, track progress |
| **Instructor** | Create courses via 5-step wizard, upload content, submit for review |
| **Manager** | Review pending course submissions, approve/reject with feedback |
| **Admin** | Full platform control, user management, category management |

## Architecture

```
                         ┌─────────────┐
                         │  Frontend    │
                         │  (React,     │
                         │   Vite)      │
                         └──────┬──────┘
                                │
                         ┌──────▼──────┐
                         │ API Gateway  │  port 8080
                         │ (Spring      │
                         │  Cloud Gw)   │
                         └──────┬──────┘
                                │
          ┌─────────────────────┼─────────────────────┐
          │                     │                     │
   ┌──────▼──────┐      ┌──────▼──────┐      ┌──────▼──────┐
   │  Sync REST  │      │  Sync REST  │      │ Async Kafka │
   │ (per-req)   │      │ (per-req)   │      │ (events)    │
   └──────┬──────┘      └──────┬──────┘      └──────┬──────┘
          │                     │                     │
   ┌──────▼──────┐      ┌──────▼──────┐      ┌──────▼──────┐
   │ user-svc    │      │ course-svc  │      │ search-svc  │
   │ port 8082   │      │ port 8083   │      │ port 8087   │
   │ (auth +     │      │ (courses,   │      │ (Elastic-   │
   │  users)     │      │  drafts,    │      │  search)    │
   └─────────────┘      │  lessons)   │      └─────────────┘
                        └──────┬──────┘      ┌─────────────┐
                               │             │ payment-svc │
   ┌─────────────┐      ┌──────▼──────┐      │ port 8086   │
   │ content-svc │      │ admin-svc   │      │ (Stripe,    │
   │ port 8084   │◄─────┤ port 8090   │      │  enroll)    │
   │ (WebFlux    │      │ (reviews,   │      └─────────────┘
   │  uploads +  │      │  reports)   │
   │  streaming) │      └─────────────┘
   └─────────────┘
```

**Communication:** REST (synchronous) for request-response flows. Kafka (asynchronous) for event-driven flows — course creation triggers search indexing, payment completion triggers enrollment.

## Tech Stack

### Backend
| Component | Technology |
|-----------|-----------|
| Language | Java 25 |
| Framework | Spring Boot 4.0.6, Spring Cloud 2025.1.1 |
| Build | Maven (multi-module) |
| Service Discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway (WebFlux) |
| Database | PostgreSQL (per service) |
| Migrations | Flyway |
| Cache | Redis |
| Search | Elasticsearch 9.0.0 |
| Messaging | Apache Kafka 7.7.1 |
| Payments | Stripe |
| Storage | Local (dev) / AWS S3 (prod) with signed URLs |
| Auth | JWT (jjwt 0.12.6), BCrypt, RBAC |

### Frontend
| Component | Technology |
|-----------|-----------|
| Language | TypeScript |
| Framework | React 19 |
| Build | Vite |
| Routing | React Router DOM |
| HTTP | Axios with JWT interceptor |
| Styling | Tailwind CSS |
| Linting | ESLint |

## Services Overview

| Service | Port | Description |
|---------|------|-------------|
| `api-gateway` | 8080 | Entry point — routes requests, validates JWT, whitelists public endpoints |
| `discovery-service` | 8761 | Eureka registry for service discovery |
| `user-service` | 8082 | Auth (register/login/refresh), user & profile management |
| `course-service` | 8083 | Course CRUD, Redis-backed drafts, sections/lessons, categories |
| `content-service` | 8084 | Chunked file upload, byte-range video streaming (WebFlux) |
| `payment-service` | 8086 | Stripe checkout, webhooks, enrollments, lesson progress |
| `search-service` | 8087 | Full-text search, faceted filters, autocomplete (Elasticsearch) |
| `admin-service` | 8090 | Course review/approval, category CRUD, manager creation |

### Databases

| Database | Owner | Key Tables |
|----------|-------|------------|
| `user_db` | user-service | users, roles, profiles, refresh_tokens |
| `course_db` | course-service | courses, sections, lessons, categories, sub_categories, video_content, text_content |
| `content_db` | content-service | media_files, upload_sessions |
| `payment_db` | payment-service | payments, enrollments, lesson_progress, refunds, payouts |
| `admin_db` | admin-service | course_reviews_queue, reports |

### Course Draft Workflow

```
Instructor → Redis Draft (5-step wizard)
    ↓ Submit for Review
Pending Review (course-service)
    ↓
Manager/Admin reviews (admin-service)
    ↓                ↓
Approved          Rejected (feedback sent)
    ↓
PostgreSQL persist → Kafka event → Elasticsearch index
```

## Getting Started

### Prerequisites

- Java 25+
- Node.js 22+
- Docker & Docker Compose
- PostgreSQL 16+
- Maven

### 1. Start Infrastructure

```bash
./start-infra.sh up
```

Starts Elasticsearch (9200), Kibana (5601), and Kafka (9092) via Docker Compose.

### 2. Start Backend Services

```bash
cd backend

# Start in order:
mvn spring-boot:run -pl discovery-service     # Eureka (port 8761)
mvn spring-boot:run -pl api-gateway            # Gateway (port 8080)
mvn spring-boot:run -pl user-service           # Auth & Users (port 8082)
mvn spring-boot:run -pl course-service         # Courses (port 8083)
mvn spring-boot:run -pl content-service        # Content (port 8084)
mvn spring-boot:run -pl payment-service        # Payments (port 8086)
mvn spring-boot:run -pl search-service         # Search (port 8087)
mvn spring-boot:run -pl admin-service          # Admin (port 8090)
```

Or use the convenience script:

```bash
cd backend && ./run-services.sh
```

### 3. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at `http://localhost:5173` with API proxy to `http://localhost:8080`.

### 4. Default Admin Account

- **Email:** admin@lms-platform.com
- **Password:** Admin@123

## Frontend Routes

| Route | Page | Access |
|-------|------|--------|
| `/login` | Login | Public |
| `/register` | Registration | Public |
| `/` | Dashboard (role-aware home) | Authenticated |
| `/courses/create` | 5-step course creation wizard | Instructor |
| `/courses/:id` | Published course landing page | Public |
| `/courses/:id/manage` | Course detail + upload per lesson | Instructor |
| `/course-player/:id` | Student course player with curriculum sidebar | Student |
| `/search` | Search with facets, filters, autocomplete | Public |
| `/manager` | Course review dashboard (approve/reject) | Manager, Admin |

## Key Features

### Course Creation Wizard
5-step form with Redis-backed auto-save at each step:
1. **Basic Info** — title, description, category, pricing
2. **Curriculum** — add/edit/reorder sections and lessons
3. **Upload** — chunked file upload per lesson (resumable, 5MB chunks)
4. **Attach** — verify uploaded files are linked to lessons
5. **Review & Submit** — final review, then submit for manager approval

### Search & Discovery
Elasticsearch-powered search with:
- Full-text search across title, description, and curriculum
- Faceted filters: category, language, level, price, duration, rating
- Autocomplete suggestions
- Custom ranking (rating × popularity × recency)

### Video Streaming
Byte-range requests via WebFlux for efficient video streaming — supports seeking and partial content responses.

### Payment & Enrollment
- Stripe Checkout session creation
- Webhook-driven enrollment (source of truth)
- Idempotency keys prevent duplicate charges
- Lesson progress tracking per student

## API Endpoints

### Public (no auth)
| Method | Path | Service | Description |
|--------|------|---------|-------------|
| POST | `/api/auth/login` | user-service | Login |
| POST | `/api/auth/register` | user-service | Register |
| POST | `/api/auth/refresh` | user-service | Refresh JWT |
| GET | `/api/courses` | course-service | List published courses |
| GET | `/api/courses/{id}` | course-service | Course details |
| GET | `/api/categories` | course-service | List categories |
| GET | `/api/search/courses` | search-service | Search courses |
| GET | `/api/search/autocomplete` | search-service | Autocomplete |
| POST | `/api/webhook/stripe` | payment-service | Stripe webhook |
| GET | `/api/content/stream/{mediaId}` | content-service | Video stream |

### Authenticated
| Method | Path | Role | Service | Description |
|--------|------|------|---------|-------------|
| POST | `/api/drafts` | INSTRUCTOR | course-service | Create draft |
| PUT | `/api/drafts/{id}` | INSTRUCTOR | course-service | Update draft |
| POST | `/api/drafts/{id}/submit` | INSTRUCTOR | course-service | Submit for review |
| GET | `/api/drafts/{id}` | INSTRUCTOR | course-service | Get draft |
| POST | `/api/upload/start` | INSTRUCTOR | content-service | Start chunked upload |
| POST | `/api/upload/chunk` | INSTRUCTOR | content-service | Upload chunk |
| POST | `/api/upload/complete` | INSTRUCTOR | content-service | Complete upload |
| POST | `/api/checkout/create-session` | STUDENT | payment-service | Create Stripe session |
| GET | `/api/reviews/pending` | MANAGER/ADMIN | admin-service | Pending reviews |
| POST | `/api/reviews/{id}/approve` | MANAGER/ADMIN | admin-service | Approve course |
| POST | `/api/reviews/{id}/reject` | MANAGER/ADMIN | admin-service | Reject course |

## Project Structure

```
udemy-clone/
├── docker-compose.yml         # ES, Kibana, Kafka
├── start-infra.sh             # Infra lifecycle script
├── backend/                   # Java microservices
│   ├── pom.xml                # Multi-module Maven parent
│   ├── common/                # Shared DTOs, enums, responses
│   ├── discovery-service/     # Eureka registry
│   ├── api-gateway/           # Spring Cloud Gateway
│   ├── user-service/          # Auth + user management
│   ├── course-service/        # Course + draft management
│   ├── content-service/       # File upload + streaming
│   ├── payment-service/       # Stripe + enrollments
│   ├── search-service/        # Elasticsearch indexing + search
│   └── admin-service/         # Course review + moderation
└── frontend/                  # React + TypeScript
    └── src/
        ├── api/               # Axios clients per domain
        ├── components/        # Shared components (ProtectedRoute)
        ├── context/           # Auth context
        ├── pages/             # Page components + wizard steps
        └── features/          # Feature modules (course, search, player)
```

## Environment Variables

Key variables in `.env`:

| Variable | Description |
|----------|-------------|
| `DB_URL_*` | PostgreSQL JDBC URLs per service |
| `DB_USERNAME_*` | Database usernames |
| `DB_PASSWORD_*` | Database passwords |
| `EUREKA_SERVER_URL` | Eureka service URL |
| `STRIPE_SECRET_KEY` | Stripe secret key |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker address |
| `AWS_ACCESS_KEY_ID` | AWS access key (prod) |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key (prod) |
| `S3_BUCKET_NAME` | S3 bucket for content (prod) |

## Development

### Code Style

- **Backend:** Controller → Service → Repository pattern. No business logic in controllers. Centralized exception handling — no try/catch in controllers.
- **Frontend:** Feature-based structure. No comments in code. Tailwind CSS for all styling.

### Key Conventions

- Each service owns its database — no shared databases, no cross-service joins
- All external traffic goes through API Gateway
- JWT required for all internal service calls, RBAC enforced per service
- Webhooks are the source of truth for payments
- All uploads go through Content Service only

### Event Topics

| Topic | Producer | Consumer(s) | Purpose |
|-------|----------|-------------|---------|
| `course.events` | course-service | search-service | Index course on approval |
| `enrollment.events` | payment-service | — | Enrollment completion |

## Scripts

| Script | Purpose |
|--------|---------|
| `./start-infra.sh up` | Start Docker containers |
| `./start-infra.sh down` | Stop Docker containers |
| `./start-infra.sh kibana` | Start Kibana |
| `cd backend && ./run-services.sh` | Start all microservices |
| `cd backend && mvn clean install` | Build all backend modules |
| `cd frontend && npm run dev` | Start frontend dev server |
| `cd frontend && npm run build` | Production build |
| `cd frontend && npm run lint` | Lint frontend code |
