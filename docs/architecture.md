# TeenyURL Architecture

## Overview
TeenyURL is a distributed URL shortener designed to convert long URLs into short, shareable links.

## High-Level Flow
1. Client sends a long URL to the backend
2. Backend generates a unique short code
3. URL mapping is saved in PostgreSQL
4. Redirect requests look up the mapping in PostgreSQL
5. Analytics are updated on redirect

Future cache flow:
- Frequently accessed mappings are cached in Redis
- Redirect requests check Redis first, then PostgreSQL

## Main Components
- API Layer
- Service Layer
- Persistence Layer
- Cache Layer
- Analytics Layer

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
- active

## Cache
Redis stores:
- short_code -> original_url
- optional metadata for frequently accessed links

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
