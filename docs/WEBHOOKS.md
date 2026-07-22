# CBT-Pulse Grid Webhook Contract

## Feature configuration

Webhook delivery is disabled by default. Production deployment must explicitly set:

```text
WEBHOOK_ENABLED=true
WEBHOOK_MASTER_KEY=<Base64-encoded 32-byte key>
```

Generate the master key outside source control with a cryptographically secure tool, such as `openssl rand -base64 32`. Never reuse the JWT secret. The application refuses to start with webhooks enabled unless the configured value is valid Base64 that decodes to exactly 32 bytes.

`WEBHOOK_ALLOW_PRIVATE_HTTP` defaults to `false`. It may be enabled only for a controlled local development smoke test. Production destinations require HTTPS on the standard HTTPS port and must resolve exclusively to public addresses. Redirects are not followed, and DNS is revalidated immediately before every attempt.

Connection, response, scan, lease, backoff, batch-size, and maximum-attempt settings are documented in `.env.example` and can be overridden independently.

## Subscription administration

Only an authenticated `INSTITUTION_ADMIN` can manage subscriptions and deliveries for their own institution. The API exposes:

- `POST /api/v1/webhooks/subscriptions`
- `GET /api/v1/webhooks/subscriptions`
- `GET /api/v1/webhooks/subscriptions/{id}`
- `PATCH /api/v1/webhooks/subscriptions/{id}/status`
- `POST /api/v1/webhooks/subscriptions/{id}/rotate-secret`
- `GET /api/v1/webhooks/deliveries`
- `POST /api/v1/webhooks/deliveries/{id}/retry`

The derived subscription signing secret is returned only by creation and rotation. Normal reads and delivery history never contain it. Receivers should retain the current secret securely and update it immediately after rotation.

## Payload

The request body is immutable UTF-8 JSON with payload version `1` and these fields:

- `payloadVersion`
- `eventId`
- `eventType`
- `institutionId`
- `examId`
- `attemptId`
- `candidateId`
- `occurredAt`
- `receivedAt`
- `riskPointsApplied`
- `totalRiskScore`
- `serverTimestamp`

It deliberately excludes credentials, tokens, device identifiers and hashes, access PINs, answer correctness, and candidate personal details.

## Signature verification

Every request contains:

```text
X-CBT-Pulse-Event-Id: <monitoring event UUID>
X-CBT-Pulse-Event-Type: <monitoring event type>
X-CBT-Pulse-Timestamp: <Unix epoch seconds>
X-CBT-Pulse-Signature: v1=<lowercase hexadecimal HMAC-SHA256>
Content-Type: application/json
```

The signature input is the UTF-8 byte sequence produced by concatenating:

```text
timestamp + "." + exactRawRequestBody
```

The receiver must preserve the body bytes exactly, compute HMAC-SHA256 with the Base64-decoded subscription secret, encode the result as lowercase hexadecimal, prepend `v1=`, and compare signatures in constant time. It should also reject timestamps outside an appropriate freshness window and deduplicate by `X-CBT-Pulse-Event-Id`, because delivery is intentionally at least once.

## Delivery lifecycle

Delivery occurs in a background worker. Monitoring transactions write durable `PENDING` outbox rows but never make external HTTP calls.

Any 2xx response becomes `SUCCEEDED`. HTTP 408, 429, 5xx, timeouts, and network failures are retried with exponential backoff. Other 4xx responses and unsafe destinations become `FAILED`. Retry exhaustion becomes `DEAD_LETTER`. With the defaults, delays after unsuccessful attempts are 10, 20, 40, 80, 160, 320, and 640 seconds, bounded by the configured maximum backoff and eight total attempts.

Institution administrators may manually reset their own `FAILED` or `DEAD_LETTER` delivery to `PENDING`. Paused subscriptions are excluded from claims until reactivated. PostgreSQL row locking and expiring leases permit safe work sharing and recovery across multiple application replicas without holding database locks during network requests.
