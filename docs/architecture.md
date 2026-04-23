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
- short_code -> original_url
- redirect mappings for frequently accessed links
- TTL based on URL expiration, or a default redirect cache TTL for non-expiring links

PostgreSQL remains the source of truth. A Redis cache hit publishes the same analytics event as a cache miss, so totals, last-access metadata, and daily rollups stay correct without blocking the redirect response.

## Rate Limiting
`InMemoryRateLimiter` protects public endpoints with configurable fixed windows:
- URL creation is limited per client IP and enabled by default
- redirect limiting is available for abuse protection and disabled by default
- client IP comes from the first `X-Forwarded-For` value when present, otherwise the remote address
- blocked requests return `429 Too Many Requests` with `Retry-After`

The limiter is behind a small `RateLimiter` interface so a Redis-backed implementation can replace the in-memory counter for multi-instance deployments.

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
Recommended:
- generate numeric unique ID
- encode to Base62
- use as short code

## Future Improvements
- custom aliases
- QR code generation
- rate limiting
- user accounts
- dashboard analytics
- Kafka/event queue for async analytics
