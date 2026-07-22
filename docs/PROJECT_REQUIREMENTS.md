# CBT-Pulse Grid Software Requirements Specification

## 1. Document purpose

This document defines the functional and non-functional requirements for CBT-Pulse Grid. It provides a shared basis for implementation, testing, deployment, and institutional acceptance.

The requirements use **shall** for mandatory behavior, **should** for recommended behavior, and **may** for optional behavior.

## 2. Product purpose and scope

CBT-Pulse Grid is an institutional computer-based testing platform for creating, delivering, monitoring, grading, and auditing examinations. It is intended for universities, colleges, training organizations, and other institutions that need controlled assessment delivery on either internet-connected infrastructure or an isolated local-area network.

The platform shall:

- Support the complete examination lifecycle from question authoring through result publication.
- Enforce institution and role boundaries.
- Protect examination data and candidate credentials.
- Record monitoring signals and provide live alerts to invigilators.
- Preserve candidate answers during concurrent, intermittent, or offline-LAN operation.
- Maintain an auditable record of security-sensitive and operational actions.

The initial product is a modular monolith. It does not require independently deployed domain microservices.

## 3. Users and roles

The platform shall support the following roles:

| Role | Primary responsibilities |
| --- | --- |
| `SUPER_ADMIN` | Operates the platform, manages institutions, oversees platform-wide configuration, and performs authorized support activities. |
| `INSTITUTION_ADMIN` | Manages an institution's users, courses, operational settings, and authorized examination resources. |
| `EXAMINER` | Authors questions, configures examinations, assigns candidates, and reviews grading outcomes within an authorized institution. |
| `INVIGILATOR` | Supervises active examinations, receives live alerts, reviews monitoring events, and records permitted interventions. |
| `STUDENT` | Views assigned examinations, completes attempts, submits answers, and views released results. |

Each user shall have one or more roles. Authorization shall consider the user's roles, account status, institution membership, and ownership of the requested resource.

## 4. Functional requirements

### 4.1 Institution management

- **FR-INS-001:** The platform shall maintain an institution record with a unique code, name, status, timestamps, and optimistic-lock version.
- **FR-INS-002:** Institution status shall support `ACTIVE`, `INACTIVE`, and `SUSPENDED`.
- **FR-INS-003:** Institution-scoped operations shall prevent access to resources owned by another institution unless explicitly performed by an authorized `SUPER_ADMIN`.
- **FR-INS-004:** Inactive or suspended institutions shall not start new examination activity.

### 4.2 Identity and access

- **FR-ID-001:** The platform shall maintain users with names, unique email addresses, password hashes, account status, roles, and optional institution and registration-number values.
- **FR-ID-002:** User status shall support `ACTIVE`, `INACTIVE`, and `LOCKED`.
- **FR-ID-003:** A registration number shall be unique within an institution when it is present.
- **FR-ID-004:** Users shall authenticate with their email address and password.
- **FR-ID-005:** Successful authentication shall provide a short-lived access token and a renewable session mechanism.
- **FR-ID-006:** The platform shall allow a user to end a renewable session and invalidate its refresh credential.
- **FR-ID-007:** Inactive and locked users shall not authenticate or continue protected operations.
- **FR-ID-008:** The platform shall provide the authenticated user with a non-sensitive representation of their identity, institution, and roles.

### 4.3 Course and question-bank management

- **FR-QB-001:** Authorized users shall create and maintain courses within their institution.
- **FR-QB-002:** Authorized examiners shall create, edit, review, and retire questions and their answer options.
- **FR-QB-003:** Questions shall be associated with an owning institution and, where applicable, a course.
- **FR-QB-004:** Question data shall support future extension for question type, difficulty, marks, explanation, media, and randomization rules.
- **FR-QB-005:** Candidates shall not receive answer keys, scoring rules, or unpublished question metadata.

### 4.4 Examination management

- **FR-EX-001:** Authorized examiners shall configure an examination's title, course, schedule, duration, instructions, availability, scoring rules, and delivery controls.
- **FR-EX-002:** Examinations shall be assignable to individual candidates or defined candidate groups.
- **FR-EX-003:** The platform shall validate that assignments, questions, and candidates belong to the examination's institution.
- **FR-EX-004:** Examination publication and start conditions shall be explicit and auditable.
- **FR-EX-005:** Question order and option order shall support controlled randomization without losing deterministic attempt records.

### 4.5 Candidate attempts and answers

- **FR-ATT-001:** An eligible candidate shall start only an examination currently available to that candidate.
- **FR-ATT-002:** The platform shall record the start, deadline, submission, and final state of each attempt.
- **FR-ATT-003:** The exact questions and ordering delivered to a candidate shall be retained for the attempt.
- **FR-ATT-004:** Candidate answers shall be saved incrementally and remain recoverable after page reload, browser restart, or temporary network interruption.
- **FR-ATT-005:** Repeated answer-save requests shall not create duplicate answers or corrupt the latest accepted answer.
- **FR-ATT-006:** The server shall be authoritative for examination timing and submission eligibility.
- **FR-ATT-007:** The platform shall automatically finalize an attempt when its permitted time expires, subject to documented recovery rules for interrupted clients.
- **FR-ATT-008:** A submitted or expired attempt shall reject unauthorized answer changes.

### 4.6 Results and grading

- **FR-RES-001:** The platform shall calculate objective-question scores consistently from the attempt's retained question and answer data.
- **FR-RES-002:** Result records shall identify the related attempt and retain sufficient information to reproduce or audit the score.
- **FR-RES-003:** Result publication shall be distinct from result calculation.
- **FR-RES-004:** Students shall access only results explicitly released to them.
- **FR-RES-005:** Authorized score adjustments shall record the actor, reason, prior value, new value, and time of change.

### 4.7 Audit trail

- **FR-AUD-001:** The platform shall record security-sensitive and operational actions in an append-oriented audit trail.
- **FR-AUD-002:** Audit records shall include the actor when known, institution context, action, target, outcome, timestamp, and relevant correlation information.
- **FR-AUD-003:** Normal application users shall not edit or delete audit records.
- **FR-AUD-004:** Authorized administrators shall be able to search audit records by time, actor, action, institution, and target.

## 5. Security requirements

- **SEC-001:** All protected REST and WebSocket operations shall require authenticated identity and explicit authorization.
- **SEC-002:** Passwords shall never be stored or logged in plain text and shall be hashed using an adaptive password-hashing algorithm such as BCrypt.
- **SEC-003:** Access tokens shall be signed, short-lived, issuer-validated, and checked for expiration before use.
- **SEC-004:** Access-token claims shall contain only the identity and authorization data needed by the backend; password hashes and other secrets shall never appear in tokens or API responses.
- **SEC-005:** Refresh credentials shall be cryptographically random. Only non-reversible hashes of refresh credentials shall be persisted.
- **SEC-006:** Refresh credentials shall expire, support revocation, and rotate after successful use. Reuse of an invalidated token shall be rejected and recorded.
- **SEC-007:** Authentication failures shall return a generic response that does not reveal whether an email address exists.
- **SEC-008:** Repeated failed authentication attempts shall be rate-limited and shall support account-protection controls.
- **SEC-009:** Account and institution status changes shall take effect for subsequent protected operations without requiring data migration.
- **SEC-010:** Role checks shall be enforced by the backend and shall not rely on hidden frontend controls.
- **SEC-011:** Institution-scoped identifiers received from a client shall be validated against the authenticated user's allowed institution context.
- **SEC-012:** Validation, authentication, authorization, and conflict failures shall return consistent JSON errors without stack traces or sensitive implementation details.
- **SEC-013:** Cross-origin access shall be restricted to explicitly configured frontend origins.
- **SEC-014:** Production traffic shall use TLS. Signing keys, database passwords, and other secrets shall be supplied through protected deployment configuration and shall not be committed to source control.
- **SEC-015:** Security-relevant events, including login failure, token revocation, role change, account lock, and proctor intervention, shall be auditable.
- **SEC-016:** JPA entities shall remain internal persistence objects and shall not be returned directly from REST APIs.
- **SEC-017:** Input records shall be validated for type, size, required fields, and permitted values before domain processing.
- **SEC-018:** Database constraints shall provide a final integrity boundary for uniqueness, required values, status values, roles, and relationships.

## 6. Offline LAN requirements

- **OFF-001:** All core examination functions shall operate when the deployment has no public internet connection.
- **OFF-002:** The React frontend, Spring Boot backend, PostgreSQL database, and required static assets shall be available from infrastructure inside the institutional LAN.
- **OFF-003:** Authentication, examination delivery, answer saving, monitoring, submission, grading, and audit recording shall not depend on an external cloud service.
- **OFF-004:** External integrations shall be optional and shall not be on the critical path for starting or completing an examination.
- **OFF-005:** Clients shall clearly indicate connection loss, continue safe client-side work where permitted, and retry recoverable requests after LAN connectivity returns.
- **OFF-006:** The server shall resolve concurrent or repeated submissions deterministically and shall communicate the accepted state to the client.
- **OFF-007:** Deployment documentation shall cover local addressing, DNS or host discovery, firewall ports, time synchronization, backup, and restore procedures.
- **OFF-008:** The deployment shall use UTC for persisted timestamps and shall maintain a reliable local time source.
- **OFF-009:** A temporary loss of internet connectivity shall not invalidate active local examination sessions.

## 7. Anti-cheat monitoring requirements

- **MON-001:** The candidate application shall detect browser-observable examination events such as loss of focus, page visibility changes, fullscreen exit when fullscreen is required, disconnect and reconnect, and abnormal submission activity.
- **MON-002:** Monitoring events shall be associated with the institution, examination, attempt, candidate, event type, server timestamp, client timestamp when available, severity, and supporting details.
- **MON-003:** Monitoring events shall be persisted before or alongside live delivery so that an invigilator can review events after reconnecting.
- **MON-004:** High-priority events shall be delivered to authorized invigilators through WebSocket notifications.
- **MON-005:** The monitoring interface shall distinguish newly received, acknowledged, and resolved events.
- **MON-006:** Invigilator acknowledgements, notes, and interventions shall be audited.
- **MON-007:** A client-generated signal shall be treated as evidence for review rather than definitive proof of misconduct.
- **MON-008:** The platform shall not automatically fail a candidate solely because of one client-side monitoring signal unless an institution has enabled an explicit, documented rule.
- **MON-009:** Candidates shall not be able to view monitoring events or other candidates' activity.
- **MON-010:** Loss of the live WebSocket connection shall not stop answer saving or examination timing; missed alerts shall be recoverable from persisted events.
- **MON-011:** Monitoring data collection and retention shall be limited to institution policy and applicable privacy requirements.

## 8. Performance and concurrency requirements

- **PERF-001:** The platform shall support many candidates taking examinations concurrently without sharing session state or answer data between users.
- **PERF-002:** Under the approved institutional load profile, routine REST requests should complete within 500 milliseconds at the 95th percentile, excluding large exports, file transfers, and explicitly asynchronous work.
- **PERF-003:** Under the approved institutional load profile, answer-save requests should complete within 300 milliseconds at the 95th percentile on a healthy LAN.
- **PERF-004:** High-priority proctor events should appear in the invigilator interface within two seconds at the 95th percentile on a healthy LAN.
- **PERF-005:** Every deployment shall define and test a capacity profile covering concurrent candidates, examinations, invigilators, answer-save frequency, and monitoring-event frequency.
- **PERF-006:** Persistent writes that change versioned records shall detect conflicting updates through optimistic locking or equivalent concurrency controls.
- **PERF-007:** Attempt, answer, result, and audit updates that must remain consistent shall execute within explicit database transaction boundaries.
- **PERF-008:** Retryable operations shall be idempotent or protected by unique keys, versions, or request identifiers.
- **PERF-009:** Database queries used during active examinations shall be indexed and shall avoid unbounded reads.
- **PERF-010:** Resource exhaustion in reporting or administration functions shall not prevent active candidates from saving answers or submitting attempts.
- **PERF-011:** Load and soak tests shall demonstrate that accepted answers are not lost, duplicated, or assigned to the wrong attempt under the approved capacity profile.

## 9. Application architecture

### 9.1 Frontend

- **ARCH-FE-001:** The user interface shall be a separate React application.
- **ARCH-FE-002:** The React application shall consume versioned REST APIs for normal operations.
- **ARCH-FE-003:** The React application shall use WebSocket connections for live invigilator alerts and monitoring updates.
- **ARCH-FE-004:** Frontend authorization controls shall improve usability but shall not replace backend enforcement.
- **ARCH-FE-005:** Frontend assets required during an examination shall be deployable within the institutional LAN.

### 9.2 Backend

- **ARCH-BE-001:** The backend shall be a Spring Boot modular monolith.
- **ARCH-BE-002:** Spring Modulith shall define and verify direct application-module boundaries.
- **ARCH-BE-003:** Modules shall communicate through deliberately exposed APIs or application events rather than direct access to another module's internal implementation.
- **ARCH-BE-004:** REST APIs shall be versioned beneath `/api/v1` until a replacement version is introduced.
- **ARCH-BE-005:** Persistence entities shall not serve as external request or response models.
- **ARCH-BE-006:** Live WebSocket delivery shall complement persisted monitoring data rather than replace it.
- **ARCH-BE-007:** The backend shall expose health information suitable for local operations and orchestration probes.

## 10. PostgreSQL and Flyway persistence requirements

- **DATA-001:** PostgreSQL shall be the system of record for institutions, identities, examinations, attempts, answers, monitoring events, results, and audit data.
- **DATA-002:** Primary domain identifiers shall use UUID values.
- **DATA-003:** Persistent records shall use UTC-capable timestamps, represented by `TIMESTAMPTZ` in PostgreSQL where applicable.
- **DATA-004:** Mutable aggregate records shall use optimistic-lock version values where concurrent changes are possible.
- **DATA-005:** Foreign keys, unique constraints, required columns, and check constraints shall enforce relational integrity.
- **DATA-006:** Institution identity shall remain explicit in institution-scoped records and queries.
- **DATA-007:** Flyway shall own production schema evolution through ordered, immutable, reviewable migrations.
- **DATA-008:** An applied migration shall not be edited. Corrections shall be introduced through a subsequent migration.
- **DATA-009:** Hibernate schema generation shall remain in validation mode for managed environments.
- **DATA-010:** Schema migrations shall complete successfully before the application is considered ready.
- **DATA-011:** Backup and restore procedures shall preserve both operational data and Flyway schema history.
- **DATA-012:** Sensitive fields shall be minimized and protected according to their classification and retention requirements.

## 11. Docker and Kubernetes requirements

- **DEP-001:** Docker shall provide reproducible images and local service composition for the frontend, backend, and PostgreSQL dependencies.
- **DEP-002:** Runtime configuration and secrets shall be supplied externally through environment variables, mounted configuration, or a secret-management mechanism.
- **DEP-003:** Container images shall be versioned, run with the minimum necessary privileges, and exclude development credentials from production releases.
- **DEP-004:** Containers shall expose health information suitable for startup, readiness, and liveness checks.
- **DEP-005:** Kubernetes deployments shall define resource requests and limits, rolling-update behavior, readiness probes, liveness probes, and startup behavior.
- **DEP-006:** PostgreSQL persistence shall use durable storage and an explicit backup policy; database replicas shall not rely on ephemeral container filesystems.
- **DEP-007:** Kubernetes networking shall expose only required services and ports.
- **DEP-008:** Deployment manifests shall support institution-hosted clusters that have restricted or no outbound internet access.
- **DEP-009:** Required images and deployment artifacts shall be exportable to, and installable from, a local registry or offline package set.
- **DEP-010:** Application instances shall remain stateless outside PostgreSQL and approved shared infrastructure so that backend replicas can be added safely.
- **DEP-011:** WebSocket routing shall preserve functional live-alert delivery when more than one backend replica is deployed.

## 12. Module responsibilities

| Module | Responsibility | Boundary expectations |
| --- | --- | --- |
| `identity` | Users, roles, account status, credentials, authentication context, and session lifecycle. | Shall not expose password hashes or persistence entities. Institution membership is represented by an institution identifier rather than an entity association across modules. |
| `institution` | Institution identity, status, settings, courses, and tenant context. | Owns institutional lifecycle and supplies explicit contracts needed by other modules. |
| `questionbank` | Questions, answer options, authoring workflow, classification, and question reuse. | Answer keys and internal authoring data remain hidden from candidate-facing contracts. |
| `examination` | Examination definitions, scheduling, rules, assignment, publication, and question selection. | Coordinates identifiers and published contracts without taking ownership of attempts or results. |
| `attempt` | Candidate sessions, delivered questions, answers, timing, autosave, submission, and attempt state. | Owns the authoritative record of what a candidate received and submitted. |
| `monitoring` | Proctor events, anti-cheat signals, live invigilator alerts, acknowledgement, and intervention records. | Persists events independently of WebSocket availability and exposes data only to authorized staff. |
| `result` | Scoring, grading outcomes, review, adjustment, and result publication. | Derives outcomes from stable attempt data and retains an auditable scoring basis. |
| `audit` | Security and operational audit records, search, retention, and integrity. | Accepts audit facts from other modules while preventing ordinary mutation or deletion. |

## 13. Acceptance criteria

The initial production-ready release shall be acceptable when all the following conditions are met:

### 13.1 Architecture and build

- **AC-001:** Spring Modulith verification completes without module-boundary violations.
- **AC-002:** The React frontend and Spring Boot backend build and deploy as separate artifacts.
- **AC-003:** No REST endpoint serializes a JPA entity or password hash.
- **AC-004:** Versioned REST endpoints and live WebSocket alert channels are documented and covered by integration tests.

### 13.2 Identity and authorization

- **AC-005:** Each defined role can perform its authorized workflows and is denied workflows outside its permissions.
- **AC-006:** Institution-scoped users cannot read or modify another institution's data.
- **AC-007:** Inactive and locked users cannot authenticate or use protected operations.
- **AC-008:** Passwords are stored only as adaptive hashes and never appear in logs, tokens, or responses.
- **AC-009:** Access-token expiry, issuer validation, refresh rotation, refresh revocation, and invalid-token rejection are verified by automated tests.
- **AC-010:** Authentication, validation, unauthorized, forbidden, conflict, and invalid-refresh failures produce consistent JSON responses.

### 13.3 Examination lifecycle

- **AC-011:** An authorized examiner can prepare, publish, assign, and schedule an examination using institution-owned questions.
- **AC-012:** An eligible student can start an assigned examination, save answers repeatedly, reconnect, and submit once without losing accepted data.
- **AC-013:** The server prevents access before availability, after expiry, or by an unassigned student.
- **AC-014:** The retained attempt record reproduces the exact question and option ordering delivered to the student.
- **AC-015:** Objective results are reproducible, remain hidden until release, and cannot be viewed by another student.

### 13.4 Monitoring and audit

- **AC-016:** Supported browser monitoring signals create persistent proctor events.
- **AC-017:** A high-priority event reaches an authorized invigilator through WebSocket within the stated healthy-LAN target under the approved load profile.
- **AC-018:** Events created while an invigilator is disconnected are available after reconnection.
- **AC-019:** Invigilator actions and security-sensitive administrative changes produce searchable audit records.
- **AC-020:** Ordinary application workflows cannot update or delete audit history.

### 13.5 Persistence and deployment

- **AC-021:** A new PostgreSQL database reaches the required schema solely by applying Flyway migrations in order.
- **AC-022:** Application startup fails readiness when required schema validation or migration fails.
- **AC-023:** Backup restoration reproduces operational records and Flyway history in a verification environment.
- **AC-024:** Docker deployment starts the required services with persistent PostgreSQL storage and passing health checks.
- **AC-025:** Kubernetes deployment passes startup, readiness, and liveness checks and supports a controlled rolling update.
- **AC-026:** The documented offline package can be installed and operated on an isolated institutional LAN without external services.

### 13.6 Performance and resilience

- **AC-027:** The approved capacity test satisfies the REST, answer-save, and live-alert percentile targets.
- **AC-028:** Concurrent answer saves and administrative updates do not lose accepted data or cross user, attempt, examination, or institution boundaries.
- **AC-029:** Temporary client disconnection and retry do not duplicate answers, submissions, results, or audit records.
- **AC-030:** A soak test covering the expected maximum examination duration completes without progressive response degradation, connection leakage, or data inconsistency.
