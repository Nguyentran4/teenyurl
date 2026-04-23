# TeenyURL

TeenyURL is a distributed URL shortener built for scalability, reliability, and fast redirects.

## Goals
- Create short URLs from long URLs
- Redirect users using short codes
- Track click analytics
- Support expiration dates
- Use caching for fast lookups
- Prepare the system for horizontal scaling

## Tech Stack
- Java 21
- Spring Boot
- PostgreSQL
- Redis
- Docker Compose
- Maven
- JUnit 5

## Project Structure
```text
teenyurl/
  README.md
  AGENTS.md
  .env.example
  .gitignore
  .codex/
    config.toml
  docs/
    architecture.md
    api.md
  backend/
```

## Milestones
- [x] Set up Spring Boot backend
- [x] Create URL shortening API
- [x] Create redirect endpoint
- [x] Save URLs in PostgreSQL
- [x] Add Redis caching
- [x] Add expiration support
- [x] Add click analytics
- [x] Add Docker Compose
- [x] Add tests
- [ ] Document architecture

## Core Features
- Shorten long URLs
- Redirect by short code
- Custom alias support
- Expiration support
- Analytics for total clicks, last access time, daily clicks, and privacy-safe request metadata
- Async analytics updates so redirects are not blocked by stats writes
- Configurable per-IP rate limiting for URL creation, with optional redirect limiting
- Consistent JSON error responses with timestamp, status, error, message, and path
- Redis cache for hot URLs
- Clean layered backend structure

## Example APIs
- `POST /api/urls`
- `GET /{shortCode}`
- `GET /api/urls/{shortCode}/stats`

## Local Development
Current MVP:
- Spring Boot app
- PostgreSQL URL storage
- Persisted click counts
- Redis redirect lookup cache

### Start PostgreSQL and Redis
From the repository root:

```powershell
docker compose up -d
```

This starts PostgreSQL on `localhost:5432` with:

```text
database: teenyurl
username: teenyurl
password: teenyurl
```

It also starts Redis on `localhost:6379`.

### Run the Backend
From `backend/`:

```powershell
.\mvnw.cmd spring-boot:run
```

The app uses these defaults:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/teenyurl
spring.datasource.username=teenyurl
spring.datasource.password=teenyurl
```

You can override them with `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`.

Redis uses these defaults:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
teenyurl.cache.redirect.default-ttl=PT1H
teenyurl.analytics.ip-hash-salt=teenyurl-local-dev
teenyurl.rate-limit.create.enabled=true
teenyurl.rate-limit.create.limit=10
teenyurl.rate-limit.create.window=PT1M
teenyurl.rate-limit.redirect.enabled=false
teenyurl.rate-limit.redirect.limit=120
teenyurl.rate-limit.redirect.window=PT1M
```

Set `TEENYURL_ANALYTICS_IP_HASH_SALT` in non-local environments so stored IP hashes cannot be compared across deployments.

### Stop Local Services
From the repository root:

```powershell
docker compose down
```

To remove local database data too:

```powershell
docker compose down -v
```

Planned local stack:
- Dockerized backend service

## Notes
This project is intended to be resume-friendly and production-inspired, with focus on distributed systems concepts such as:
- caching
- horizontal scaling
- unique ID generation
- database consistency
- rate limiting
