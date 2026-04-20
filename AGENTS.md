# AGENTS.md

## Project
Build TeenyURL, a distributed URL shortener with clean architecture and production-style engineering practices.

## Primary goals
- Create short URLs
- Redirect quickly and reliably
- Store data in PostgreSQL
- Cache hot mappings in Redis
- Track basic analytics
- Design with distributed scaling in mind

## Stack
- Java 21
- Spring Boot
- Maven
- PostgreSQL
- Redis
- Docker Compose
- JUnit 5

## Code style
- Prefer simple, readable code
- Use clear names
- Keep methods focused
- Avoid unnecessary dependencies
- Favor maintainability over cleverness

## Backend structure
Use this layered structure:
- controller
- service
- repository
- model/entity
- dto
- config
- exception

## Rules
- Do not change the stack unless necessary
- Do not add major libraries without explaining why
- Keep APIs RESTful
- Validate request inputs
- Add tests for service logic
- Add integration tests for main endpoints
- Update README when behavior changes
- Update docs when architecture changes

## Workflow
When asked to implement something:
1. Explain the plan briefly
2. Implement in small steps
3. Keep files organized
4. Add or update tests
5. Summarize what changed

## Feature priorities
1. Single-node MVP first
2. Database persistence
3. Redis caching
4. Expiration support
5. Analytics
6. Distributed-system improvements
7. Rate limiting / abuse protection

## Short code generation
Preferred order:
1. Base62 encoded unique ID
2. Avoid collisions
3. Keep code short and URL-friendly

## Distributed system expectations
Design should be compatible with:
- multiple application instances
- shared database
- shared Redis cache
- load balancer
- scalable ID generation strategy

## Deliverables
- working backend
- clear folder structure
- test coverage for core flows
- Docker-based local setup
- architecture documentation
