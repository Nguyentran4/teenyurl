# TeenyURL API

## Create Short URL
**POST** `/api/urls`

### Request body
```json
{
  "originalUrl": "https://example.com/very/long/link",
  "expiresAt": "2026-12-31T23:59:59"
}
```

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
- Increment click count

## Get URL Stats
**GET** `/api/urls/{shortCode}/stats`

### Response
```json
{
  "shortCode": "abc123",
  "originalUrl": "https://example.com/very/long/link",
  "clickCount": 42,
  "createdAt": "2026-04-18T12:00:00",
  "expiresAt": "2026-12-31T23:59:59"
}
```

## Optional Future APIs
- `DELETE /api/urls/{shortCode}`
- `POST /api/urls/custom`
- `GET /api/urls`
