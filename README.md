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
    Dockerfile
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
- Health checks for database and Redis readiness
- Basic service metrics for uptime, URL totals, click totals, and JVM runtime
- Configurable per-IP rate limiting for URL creation, with optional redirect limiting
- Consistent JSON error responses with timestamp, status, error, message, and path
- Snowflake-style Base62 short code generation for compact, multi-instance-safe IDs
- Redis cache for hot URLs
- Clean layered backend structure

## Example APIs
- `POST /api/urls`
- `GET /{shortCode}`
- `GET /api/urls/{shortCode}/stats`
- `GET /health`
- `GET /metrics`

## Docker Compose
The full local stack runs with Docker Compose:

- Spring Boot app on `localhost:8080`
- PostgreSQL on `localhost:5432`
- Redis on `localhost:6379`

### Configure Environment
Create a local `.env` file from the example when you want to override defaults:

```powershell
Copy-Item .env.example .env
```

Important defaults:

```text
APP_PORT=8080
POSTGRES_DB=teenyurl
POSTGRES_USER=teenyurl
POSTGRES_PASSWORD=teenyurl
POSTGRES_PORT=5432
REDIS_PORT=6379
TEENYURL_ANALYTICS_IP_HASH_SALT=teenyurl-local-dev
TEENYURL_SHORT_CODE_NODE_ID=0
TEENYURL_CREATE_API_KEY=
```

Inside Docker, the app connects to PostgreSQL at `postgres:5432` and Redis at `redis:6379` using the Compose service names. The database and Redis ports are also published to localhost for debugging.

### Start The Full Stack
From the repository root:

```powershell
docker compose up --build
```

Run it in the background with:

```powershell
docker compose up --build -d
```

Check that the app is ready:

```powershell
Invoke-RestMethod http://localhost:8080/health
```

Create a short URL:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/urls `
  -ContentType "application/json" `
  -Body '{"originalUrl":"https://example.com/articles/123"}'
```

If `TEENYURL_CREATE_API_KEY` is set, include `X-API-Key` on create requests.

### View Logs
```powershell
docker compose logs -f app
```

### Stop The Stack
```powershell
docker compose down
```

To remove local database and Redis data too:

```powershell
docker compose down -v
```

## Local Development Without App Container
You can also run only PostgreSQL and Redis in Docker while running Spring Boot directly from your IDE:

- Spring Boot app
- PostgreSQL URL storage
- Persisted click counts
- Redis redirect lookup cache

### Start PostgreSQL And Redis
From the repository root:

```powershell
docker compose up -d postgres redis
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
teenyurl.cache.redirect.key-prefix=url:
teenyurl.cache.redirect.default-ttl=PT1H
teenyurl.analytics.ip-hash-salt=teenyurl-local-dev
teenyurl.rate-limit.create.enabled=true
teenyurl.rate-limit.create.limit=10
teenyurl.rate-limit.create.window=PT1M
teenyurl.rate-limit.redirect.enabled=false
teenyurl.rate-limit.redirect.limit=120
teenyurl.rate-limit.redirect.window=PT1M
teenyurl.rate-limit.redis.enabled=true
teenyurl.rate-limit.redis.key-prefix=rate_limit:
teenyurl.short-code.snowflake.node-id=0
teenyurl.short-code.snowflake.epoch-millis=1735689600000
teenyurl.security.create-api-key=
teenyurl.security.allow-private-redirect-targets=false
```

Set `TEENYURL_ANALYTICS_IP_HASH_SALT` in non-local environments so stored IP hashes cannot be compared across deployments.
Set a unique `TEENYURL_SHORT_CODE_NODE_ID` for each application instance in multi-instance deployments.
Redis key spaces are separated by purpose: redirect cache entries use `url:{shortCode}` and rate limiting uses `rate_limit:{action}:{clientIp}`.

### Security
TeenyURL validates and sanitizes redirect targets before storing them:

- Only absolute `http` and `https` URLs are accepted.
- URL schemes and hosts are normalized before persistence.
- User info, whitespace, control characters, relative URLs, and protocol-relative URLs are rejected.
- Localhost and private/local literal IP targets are rejected by default to reduce open redirect and internal-network abuse.
- Custom aliases are trimmed and restricted to letters, numbers, hyphens, and underscores.

Set `TEENYURL_ALLOW_PRIVATE_REDIRECT_TARGETS=true` only for trusted internal deployments that need short links to private network targets.

URL creation can be protected with a shared API key:

```powershell
$env:TEENYURL_CREATE_API_KEY="replace-with-a-long-random-secret"
```

When configured, clients must include the key on create requests:

```text
X-API-Key: replace-with-a-long-random-secret
```

### Production Readiness
TeenyURL exposes lightweight operational endpoints without requiring Spring Actuator:

```text
GET /health
GET /metrics
```

`/health` validates database connectivity with a simple query and validates Redis with `PING` when Redis-backed features are enabled. It returns HTTP 200 with status `UP` when required dependencies are available, and HTTP 503 with status `DOWN` when a required dependency fails. If Redis-backed caching and Redis-backed rate limiting are disabled, Redis is reported as `DISABLED`.

`/metrics` returns basic process and application counters:

```text
startedAt
uptimeSeconds
urls.totalUrls
urls.totalClicks
runtime.availableProcessors
runtime.usedMemoryBytes
runtime.maxMemoryBytes
```

Production deployment notes:
- Run multiple application instances behind a load balancer.
- Use the same PostgreSQL database and Redis deployment across all instances.
- Set a unique `TEENYURL_SHORT_CODE_NODE_ID` per instance to avoid generated ID overlap.
- Set a strong, deployment-specific `TEENYURL_ANALYTICS_IP_HASH_SALT`.
- Keep Redis enabled for shared redirect caching and distributed rate limiting.
- Use `/health` as the readiness probe and monitor `/metrics` for basic service trends.
- Restrict public access to `/metrics` at the ingress or network layer if needed.

## Notes
This project is intended to be resume-friendly and production-inspired, with focus on distributed systems concepts such as:
- caching
- horizontal scaling
- unique ID generation
- database consistency
- rate limiting
