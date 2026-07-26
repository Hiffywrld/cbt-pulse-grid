# Microservices Phase 4 and Phase 5: Examination and Monitoring Extraction

CBT-Pulse Grid now exposes separately runnable service containers for the remaining major backend boundaries while preserving the existing public API paths through `api-gateway`.

## Services

### api-gateway

The gateway is the public backend entry point. It preserves current frontend URLs and routes traffic by path to the owning service.

### identity-service

Owns authentication, users, roles, institutions, profiles, passwords, avatars, refresh tokens, JWT issuing, and bootstrap admin.

### examination-service

Owns the academic and exam lifecycle:

- subjects
- question bank
- exam creation, edit, publish, cancel and close
- candidate assignment
- student assigned exams
- attempt start and resume
- answer autosave/offline sync
- submit and auto-submit
- scoring
- results, absence reporting and CSV export

### monitoring-service

Owns live and asynchronous proctoring workflows:

- student heartbeat
- monitoring event batches
- risk scoring
- missed-heartbeat worker
- STOMP WebSocket endpoint `/ws`
- live monitoring dashboard
- monitoring event history
- webhook subscriptions
- webhook delivery, retry and secret rotation

## Gateway route ownership

Identity routes:

- `/api/v1/auth/**`
- `/api/v1/users/**`
- `/api/v1/institutions/**`

Examination routes:

- `/api/v1/subjects/**`
- `/api/v1/questions/**`
- `/api/v1/exams/**`
- `/api/v1/student/exams/**`
- `/api/v1/student/attempts/**` except monitoring-specific subroutes
- `/api/v1/results/**`

Monitoring routes:

- `/api/v1/student/attempts/{id}/heartbeat`
- `/api/v1/student/attempts/{id}/monitoring-events`
- `/api/v1/monitoring/**`
- `/api/v1/webhooks/**`
- `/ws`

Monitoring-specific student attempt routes are intentionally defined before the generic attempt route.

## Database compatibility

Phase 4/5 keeps the same PostgreSQL instance and default schema. This avoids destructive data movement during service extraction and preserves the existing Flyway history.

Logical table ownership is now:

- Identity: `institutions`, `users`, `user_roles`, `refresh_tokens`
- Examination: `subjects`, `questions`, `question_options`, `exams`, `exam_pool_rules`, `exam_candidates`, `exam_attempts`, `attempt_questions`, `attempt_options`, `attempt_answers`, `attempt_answer_selections`, `attempt_sync_batches`
- Monitoring: `monitoring_states`, `monitoring_events`, `monitoring_sync_batches`, `monitoring_heartbeat_receipts`, `webhook_subscriptions`, `webhook_subscription_event_types`, `webhook_deliveries`
- Audit remains shared technical debt until it is extracted into its own service or event stream.

The current schema still contains cross-boundary foreign keys. They are retained for safety and data integrity in this phase. Phase 6 should replace cross-service foreign keys with service-owned IDs, internal APIs, and asynchronous consistency where appropriate.

## Security and identity access

All extracted services validate JWTs locally using the same issuer and shared secret for now. JWT claims remain stable: `sub`, `email`, `roles`, `institutionId`, `iss`, `iat`, `exp`, and `jti`.

For this phase, examination-service and monitoring-service continue to rely on UUIDs and role/institution claims plus shared database compatibility adapters. Full internal HTTP clients to identity-service are deferred to Phase 6 because changing every authorization and tenant lookup in one step would be higher risk than the route-level extraction.

## Compatibility adapters

The service directories intentionally carry enough existing backend code to remain independently runnable and testable while gateway routing enforces public ownership. This avoids breaking frontend behavior during extraction.

Known temporary limitations:

- same PostgreSQL database and schema
- shared Flyway migration history
- duplicated compatibility code across services
- old monolith endpoints still exist behind the fallback backend route
- internal service-to-service HTTP clients are not fully introduced yet
- cross-service foreign keys remain in the database

These limitations are explicit technical debt for Phase 6 rather than hidden coupling.