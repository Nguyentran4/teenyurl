# TeenyURL Architecture

## Overview
TeenyURL is a distributed URL shortener designed to convert long URLs into short, shareable links.

## High-Level Flow
1. Client sends a long URL to the backend
2. Backend generates a unique short code
3. URL mapping is saved in PostgreSQL
4. Redirect requests check Redis for the short code mapping
5. On cache miss, the service loads the mapping from PostgreSQL and stores it in Redis
6. Redirect handling publishes an analytics event and returns without waiting for analytics writes
7. An async analytics worker updates PostgreSQL in the background

## Main Components
- API Layer
- Service Layer
- Persistence Layer
- Cache Layer
- Analytics Layer
- Rate Limiting Layer

## Proposed Backend Layers
- controller: request handling
- service: business logic
- repository: database access
- model/entity: persistence models
- dto: request and response models
- config: application configuration
- exception: error handling

## Database
Main table:
- id
- original_url
- short_code
- created_at
- expires_at
- click_count
- last_accessed_at
- last_user_agent
- last_referrer
- last_ip_hash
- active

Daily analytics table:
- id
- url_mapping_id
- access_date
- click_count

## Cache
Redis stores:
- `url:{shortCode}` -> original_url
- redirect mappings for frequently accessed links
- TTL based on URL expiration, or a default redirect cache TTL for non-expiring links

PostgreSQL remains the source of truth. A Redis cache hit publishes the same analytics event as a cache miss, so totals, last-access metadata, and daily rollups stay correct without blocking the redirect response.
Write paths evict the corresponding redirect cache key so future mapping updates stay consistent.

## Rate Limiting
Rate limiting uses configurable fixed windows:
- URL creation is limited per client IP and enabled by default
- redirect limiting is available for abuse protection and disabled by default
- client IP comes from the first `X-Forwarded-For` value when present, otherwise the remote address
- blocked requests return `429 Too Many Requests` with `Retry-After`
- Redis-backed counters use `rate_limit:{action}:{clientIp}` keys with TTL matching the configured window

The limiter is behind a small `RateLimiter` interface. `RedisRateLimiter` is the production default for shared limits across instances, while `InMemoryRateLimiter` remains the fallback when Redis rate limiting is disabled.

## Analytics
Redirects publish a lightweight in-process event. `AnalyticsService` handles that event asynchronously with its own transaction and updates:
- total click count on the URL mapping
- last accessed timestamp
- daily click rollup by UTC date
- latest user-agent and referrer
- salted SHA-256 hash of the client IP, derived from `X-Forwarded-For` when present

The current implementation uses Spring async with a bounded single-worker queue to keep redirect latency low and avoid in-process rollup races. The event boundary can later be replaced by a durable queue such as Kafka without changing redirect controller behavior.

## Scaling Considerations
- multiple backend instances behind a load balancer
- shared PostgreSQL database
- shared Redis cache
- stateless application servers
- unique ID generation safe across instances

## Short Code Strategy
Current implementation:
- generate a Snowflake-style 64-bit ID using timestamp, node id, and per-millisecond sequence
- configure each app instance with a unique `teenyurl.short-code.snowflake.node-id`
- encode the numeric ID to Base62 for compact URL-friendly short codes

Tradeoffs:
- compact output and no database round-trip for code generation
- safe for horizontal scaling when node ids are configured correctly
- operationally simpler than Kafka or an external ID service
- requires unique node-id assignment per instance; misconfiguration can still cause collisions

## Future Improvements
- custom aliases
- QR code generation
- rate limiting
- user accounts
- dashboard analytics
- Kafka/event queue for async analytics
