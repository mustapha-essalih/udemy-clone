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


### Start Backend Services

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
- 
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

### Key Conventions

- Each service owns its database — no shared databases, no cross-service joins
- All external traffic goes through API Gateway
- JWT required for all internal service calls, RBAC enforced per service
- Webhooks are the source of truth for payments
- All uploads go through Content Service only
