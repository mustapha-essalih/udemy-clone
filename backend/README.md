# Backend — Course Studio

Java Spring Boot microservices for the LMS platform. See the [root README](../README.md) for full documentation.

## Services

| Service | Port | Description |
|---------|------|-------------|
| `discovery-service` | 8761 | Eureka service registry |
| `api-gateway` | 8080 | Spring Cloud Gateway |
| `user-service` | 8082 | Auth & user management |
| `course-service` | 8083 | Course CRUD, drafts, categories |
| `content-service` | 8084 | Chunked uploads, video streaming |
| `payment-service` | 8086 | Stripe, enrollments, progress |
| `search-service` | 8087 | Elasticsearch search & autocomplete |
| `admin-service` | 8090 | Course review & moderation |

## Build & Run

```bash
mvn clean install
mvn spring-boot:run -pl discovery-service
mvn spring-boot:run -pl api-gateway
# ... remaining services
```

Or use `./run-services.sh` to start all services in separate terminal tabs.
