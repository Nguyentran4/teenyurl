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
- [ ] Save URLs in PostgreSQL
- [ ] Add Redis caching
- [ ] Add expiration support
- [x] Add click analytics
- [ ] Add Docker Compose
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
- In-memory URL storage
- In-memory click counts

Planned local stack:
- PostgreSQL
- Redis
- Docker Compose

## Notes
This project is intended to be resume-friendly and production-inspired, with focus on distributed systems concepts such as:
- caching
- horizontal scaling
- unique ID generation
- database consistency
- rate limiting
