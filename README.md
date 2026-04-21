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
- [ ] Add Redis caching
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
- Analytics for total clicks
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

### Start PostgreSQL
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

### Stop PostgreSQL
From the repository root:

```powershell
docker compose down
```

To remove local database data too:

```powershell
docker compose down -v
```

Planned local stack:
- Redis

## Notes
This project is intended to be resume-friendly and production-inspired, with focus on distributed systems concepts such as:
- caching
- horizontal scaling
- unique ID generation
- database consistency
- rate limiting
