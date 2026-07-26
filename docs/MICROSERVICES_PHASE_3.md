# Microservices Phase 3: Identity Service Extraction

CBT-Pulse Grid now has a separately runnable `identity-service` while preserving the existing public API paths through `api-gateway`.

## Extracted responsibility

`identity-service` owns these runtime capabilities:

- authentication login, refresh, logout and `/auth/me`
- JWT issuing with the existing claims contract: `sub`, `email`, `roles`, `institutionId`, `iss`, `iat`, `exp`, `jti`
- refresh-token persistence and rotation
- users and role assignments
- institution management
- profile update, avatar selection and password change
- bootstrap SUPER_ADMIN creation and one-time forced recovery

## Gateway routing

The gateway routes these existing public paths to `identity-service`:

- `/api/v1/auth/**`
- `/api/v1/users/**`
- `/api/v1/institutions/**`

All other `/api/**` paths continue to route to the existing backend service. `/ws` also continues to route to the existing backend for live monitoring.

## Database compatibility

Phase 3 intentionally keeps the same PostgreSQL instance and existing default schema. No data is removed and the existing Flyway history is preserved.

Because the current monolith already uses one shared `flyway_schema_history` table, `identity-service` carries the existing migration set for compatibility validation. The identity-owned tables are:

- `institutions`
- `users`
- `user_roles`
- `refresh_tokens`

The `users.avatar_key` profile/avatar extension is also identity-owned. The audit tables remain a compatibility dependency for identity/institution audit writes until audit is extracted or converted to an event-driven integration.

## Compatibility adapters

The original backend still contains duplicate identity and institution controllers. They are retained as a rollback/compatibility path, but gateway routing makes the extracted service authoritative for identity public traffic.

Future phases should replace in-process module calls with service-to-service APIs or events before removing duplicate monolith handlers.

## Known limitations

- Database-per-service is not implemented yet.
- Identity service still shares the monolith package namespace to minimize frontend/API risk during extraction.
- Existing backend services validate JWTs with the same shared secret and issuer for now.
- Audit writes are still direct database writes from copied compatibility code, not asynchronous events.
