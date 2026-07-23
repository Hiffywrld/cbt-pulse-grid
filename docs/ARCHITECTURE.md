# CBT-Pulse Grid Architecture

## Architectural style

CBT-Pulse Grid is designed as a modular monolith. The backend is deployed as one Spring Boot application, while Spring Modulith establishes explicit domain boundaries within that application. This approach keeps development, transactions, testing, and offline deployment straightforward without sacrificing the option to extract modules into independent services later if scale or organizational needs justify it.

Modules should collaborate through deliberately exposed APIs and application events. Their internal types remain private to their owning modules. Entities, repositories, controllers, and business workflows will be introduced inside these boundaries in later development stages.

## Application modules

- **Identity and Access Management (`identity`)** manages planned users, roles, authentication context, authorization policies, and account lifecycle.
- **Institution Management (`institution`)** manages institutional ownership, courses, organizational settings, and tenant-level context.
- **Question Bank (`questionbank`)** manages questions, answer options, course-aligned question collections, and future question authoring workflows.
- **Examination Management (`examination`)** manages exam definitions, scheduling, configuration, assignments, and publication lifecycle.
- **Exam Attempts (`attempt`)** manages candidate sessions, allocated questions, submitted answers, timing, and attempt lifecycle.
- **Live Exam Monitoring (`monitoring`)** manages proctoring signals, anti-cheat events, and live invigilator notifications.
- **Results and Grading (`result`)** manages scoring, grading outcomes, result calculation, and publication.
- **Audit Trail (`audit`)** records security-sensitive and operational actions for accountability and investigation.

## System boundaries and communication

The React application is a separate frontend and is not embedded in the Spring Boot backend. It communicates with the backend through REST APIs for normal operations such as administration, exam setup, question management, candidate activity, and results. WebSocket connections are reserved for time-sensitive live invigilator alerts and monitoring updates.

PostgreSQL is the system of record. Module ownership rules should be reflected in the data model even though the modules share one database in the modular-monolith deployment.

## Deployment and offline operation

Docker provides reproducible local and server deployments of the backend, frontend, and PostgreSQL services. Kubernetes is the target orchestration platform for deployments that require scheduling, health management, scaling, and resilient service operation.

The platform must also operate on an offline institutional LAN. During offline operation, browsers connect to services hosted within the local network, and all core examination, monitoring, and persistence functions remain independent of public internet connectivity. External integrations must therefore be optional and must not sit on the critical exam-delivery path.

## Live monitoring WebSocket contract

The future React invigilator dashboard connects directly to the native WebSocket/STOMP endpoint at `/ws`; SockJS is not required. Its STOMP `CONNECT` frame must contain the current access token in this native header:

```text
Authorization: Bearer <access-token>
```

After the connection is authenticated, an authorized institutional staff user subscribes to:

```text
/topic/exams/{examId}/monitoring
```

The backend revalidates the current account, staff roles, institution claim, and exam ownership for every subscription. Monitoring updates are published only after their database transaction commits and contain dashboard-safe candidate and attempt state; they never contain device identifiers or hashes, answer correctness, access PINs, credentials, or tokens. Allowed browser origins are configured with `MONITORING_WEBSOCKET_ALLOWED_ORIGINS`, which defaults to `http://localhost:5173` for local development.

## Final backend operations

Expired attempts are finalized by a configurable scheduled worker. Each transaction claims a bounded batch of eligible `IN_PROGRESS` rows with PostgreSQL `FOR UPDATE SKIP LOCKED`, applies the same idempotent scoring path used by manual submission, and commits before the next batch. This permits multiple replicas to share work without duplicate scoring and prevents logically expired attempts from generating missed-heartbeat incidents.

The result module is a read model over finalized attempt snapshots and exam assignments. It uses bounded aggregate and paginated SQL queries so assigned candidates who never started remain visible and result pages do not introduce entity-graph N+1 queries. Objective answer review is available only for finalized attempts and only to authorized staff in the owning institution.

The audit module owns an append-only institutional event log. Domain services write audit records within their existing transactions, including the authenticated actor, sorted roles, action, target, outcome, timestamp, request correlation identifier, and tightly bounded non-sensitive metadata. The database rejects update and delete operations, and the repository intentionally exposes no deletion API.

Every REST request receives a UUID correlation identifier in `X-Request-Id`. A valid caller-supplied UUID is preserved; an absent value is generated; malformed or ambiguous values are rejected. The identifier is returned in response headers and JSON errors and is included in server log context. REST CORS origins are independently configured with `REST_CORS_ALLOWED_ORIGINS`.

Actuator exposes only health and info through the web endpoint set. Liveness and readiness probe groups are enabled, health details and component structure are suppressed, and existing security rules keep health probes public while protecting info.
