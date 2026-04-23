# TeenyURL API

## Create Short URL
**POST** `/api/urls`

### Request body
```json
{
  "originalUrl": "https://example.com/very/long/link",
  "alias": "optional-custom-name",
  "expiresAt": "2026-12-31T23:59:59"
}
```

`alias` is optional. If provided, it must be unique and contain only letters, numbers, hyphens, or underscores.

### Response
```json
{
  "shortCode": "abc123",
  "shortUrl": "http://localhost:8080/abc123",
  "originalUrl": "https://example.com/very/long/link",
  "expiresAt": "2026-12-31T23:59:59"
}
```

## Redirect
**GET** `/{shortCode}`

### Behavior
- Look up short code
- Redirect to original URL
- Publish an analytics event
- Return without waiting for analytics persistence

Analytics are eventually updated in the background:
- click count
- last access time
- daily click rollup
- latest user-agent, referrer, and salted IP hash when available

## Get URL Stats
**GET** `/api/urls/{shortCode}/stats`

### Response
```json
{
  "shortCode": "abc123",
  "originalUrl": "https://example.com/very/long/link",
  "clickCount": 42,
  "createdAt": "2026-04-18T12:00:00",
  "expiresAt": "2026-12-31T23:59:59",
  "lastAccessedAt": "2026-04-20T09:15:00",
  "analytics": {
    "totalClicks": 42,
    "lastAccessedAt": "2026-04-20T09:15:00",
    "dailyClicks": [
      {
        "date": "2026-04-20",
        "count": 12
      }
    ],
    "lastRequest": {
      "userAgent": "Mozilla/5.0",
      "referrer": "https://referrer.example/home",
      "ipHash": "64-character-sha256-hex"
    }
  }
}
```

`clickCount` remains available as the total for simple clients. Structured analytics are returned under `analytics`.

## Optional Future APIs
- `DELETE /api/urls/{shortCode}`
- `GET /api/urls`
