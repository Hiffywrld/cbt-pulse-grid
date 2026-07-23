# CBT-Pulse Grid Frontend Plan

## Purpose and guardrails

This document locks the frontend product boundaries for CBT-Pulse Grid. New screens must follow the backend's role and tenant rules, use candidate-safe contracts, and preserve offline-first examination delivery. Frontend visibility improves usability but never replaces backend authorization.

Phase 1 provides authentication, session restoration, route protection, navigation, responsive layout, connectivity awareness, and reusable UI foundations. Phase 2 connects platform administration, tenant user management, role dashboards, and candidate-safe assigned-exam discovery to the real backend contracts. Phase 3 connects tenant academic content and staff examination preparation, including lifecycle and candidate assignment.

## Locked role and page matrix

| Page or workspace | `SUPER_ADMIN` | `INSTITUTION_ADMIN` | `EXAMINER` | `INVIGILATOR` | `STUDENT` |
| --- | --- | --- | --- | --- | --- |
| Platform dashboard | Full | No | No | No | No |
| Institution management | Manage | No | No | No | No |
| Institution dashboard | No | Full | Full | Full | No |
| Institutional user accounts | Manage institution administrators across tenants | Manage staff and students within institution | No | No | No |
| Subjects | No | Manage | Read | No | No |
| Question bank | No | Manage | Manage | No | No |
| Staff exam management | No | Manage | Manage | Read published details/candidates | No |
| Live monitoring | No | Institution staff view | Institution staff view | Institution staff view | No |
| Staff results | No | View/export | View/export | View/export | No |
| Audit history | No | View within institution | No | No | No |
| Student dashboard | No | No | No | No | Full |
| Assigned exams and attempts | No | No | No | No | Own assignments/attempts only |
| Student results | No | No | No | No | Own submitted result only |

`SUPER_ADMIN` remains outside institutional academic-content and exam-management workflows. Institution identity always comes from the authenticated backend context, never an editable browser value. If a user has multiple roles, navigation may expose the union of valid pages, while every request remains backend-authorized.

## Responsive behaviour

- At wide desktop widths, the authenticated shell uses a persistent deep-emerald sidebar, top bar, page heading, and content workspace.
- On smaller desktop widths, the sidebar collapses to an icon rail while preserving accessible labels through tooltips/titles.
- On tablets and phones, the persistent sidebar becomes a keyboard-accessible navigation drawer with a dismissible backdrop. Primary identity and logout controls remain available in the top bar.
- Forms use single-column layouts on narrow screens, large touch targets, visible labels, and field-level errors.
- Data-heavy future pages will use responsive tables with deliberate column priority or card views; they must not rely on horizontal viewport overflow as the primary mobile experience.
- Motion remains restrained and honors `prefers-reduced-motion`.

## Frontend delivery phases

1. **Foundation and authentication** — environment validation, typed native-fetch client, refresh rotation, protected/role routes, application shell, connectivity state, design system, and test infrastructure.
2. **Platform, identity, and assigned-exam portals (completed)** — institution lifecycle management and institution-administrator provisioning for `SUPER_ADMIN`; tenant-safe staff/student account management for `INSTITUTION_ADMIN`; real role dashboards; student identity, assigned-exam availability, details, instructions, and a secure PIN-step hand-off. The PIN is not submitted until the full runner exists because successful backend validation starts the authoritative attempt timer.
3. **Academic content and exam preparation (completed)** — responsive subject management and examiner read access; staff-only question authoring with answer-structure validation and publishing; exam schedules, pool rules, pass marks, secure PIN rotation, lifecycle transitions, and paginated candidate assignment. Invigilators receive published read-only exam/candidate access. The current identity contract limits eligible-student search to institution administrators, so examiners cannot independently search candidates until the backend exposes a narrow examination-facing candidate-search contract.
4. **Candidate delivery** — PIN/device start, secure attempt package, autosave, resume, timer, and submission. Assigned-exam discovery and instructions were completed in Phase 2.
5. **Invigilation and reporting** — heartbeat and browser-event capture, authenticated STOMP dashboard, monitoring history, result reporting, CSV export, and audit history.
6. **Operational refinement** — accessibility audit, performance profiling, deployment-aware configuration, offline recovery drills, and end-to-end institutional acceptance tests.

Later phases may integrate only backend capabilities that exist and must keep DTOs separate where candidate safety requires it.

## Offline answer flow

1. The candidate starts or resumes an attempt through the server while within the published exam window and on the device locked by the backend.
2. The received candidate package contains attempt question and option identifiers only; it never includes source identifiers, correct-answer flags, PIN hashes, or scoring details.
3. The frontend stores the active attempt package, local selections, monotonically increasing per-question `clientSequence` values, and pending sync batches in IndexedDB. Tokens remain in `sessionStorage`, not IndexedDB.
4. Every autosave batch has a client-generated `syncId`. A batch stays pending until the server acknowledges it.
5. Reconnection sends pending batches in creation order. Server acknowledgement is authoritative: duplicate `syncId` values are safe, and lower/equal sequences cannot replace newer server answers.
6. Browser refresh or restart reconstructs the UI from the local package, then reconciles with the server before permitting further changes when connectivity exists.
7. Local storage is cleared or safely retired after the final submitted result is confirmed. The UI never treats a local submission marker as proof of server submission.

## Server-authoritative timer

- The server supplies `serverTime`, `expiresAt`, and `remainingSeconds`; the frontend derives a display offset from those values.
- The countdown uses monotonic browser time for smooth display but periodically reconciles with fresh server responses. Changing the device clock cannot extend an attempt.
- The effective deadline is already computed by the backend as the earlier of exam-window end and start time plus duration.
- At zero, answer editing stops immediately and the frontend requests submission when reachable. The backend's scheduled worker remains the final authority and auto-submits abandoned expired attempts even without a client request.
- Resume responses can report an already auto-submitted attempt; the frontend must transition to the result state rather than reopening answers.

## Anti-cheat browser events

The candidate client may collect only the monitoring signals accepted by the backend: tab hidden, window blur, fullscreen exit, copy attempt, paste attempt, suspected developer tools, network disconnected, and network reconnected. Heartbeats include focus, fullscreen, connectivity, client sequence, client timestamp, and the device ID used for the attempt.

- Event batches use client-generated event IDs and a batch `syncId`, allowing offline capture and idempotent replay.
- Risk weights are never calculated or sent by the frontend. The backend owns all risk scoring and caps.
- Network loss/recovery is operational context and carries zero risk in this offline-first system.
- Raw device identifiers are sent only where required by the attempt/heartbeat contract, never persisted in browser logs, analytics, UI, or error text.
- The UI must not claim that browser heuristics prove cheating; it presents them as proctoring signals for authorized review.

## Live-monitoring experience

Authorized institution staff connect to the native STOMP endpoint configured by `VITE_WS_URL`. The STOMP `CONNECT` frame carries `Authorization: Bearer <access-token>`, and subscriptions use `/topic/exams/{examId}/monitoring` only after the staff REST context confirms tenant access.

The dashboard will:

- Show safe candidate identity, attempt status, last heartbeat, online/offline state, focus/fullscreen state, event count, and backend risk score.
- Merge live updates into an initial REST dashboard snapshot and visibly mark stale connectivity without fabricating events.
- Reconnect with bounded backoff and refetch the REST snapshot after reconnect so missed WebSocket messages cannot leave stale state.
- Keep filtering and ordering usable during high-concurrency exams and announce important state changes accessibly without overwhelming screen readers.
- Never show device identifiers/hashes, answer keys, access PIN data, credentials, or tokens.

WebSocket messages are an immediacy channel, not the system of record. PostgreSQL-backed REST state remains authoritative.
## Phase 4 implementation lock

Phase 4 delivers the candidate runner through the existing student attempt APIs. PIN validation starts or resumes an attempt, and the raw PIN is held only in component memory for the duration of the request. A locally generated device identifier is retained only to satisfy the backend device lock; the backend stores its SHA-256 hash.

The runner uses `serverTime` and `expiresAt` to calculate clock drift. Its one-second countdown is display-only: refreshes never reset the authoritative backend expiry, and an expired attempt is submitted or retrieved through the backend’s idempotent submission/result APIs.

Offline answer state is stored in IndexedDB under `cbt-pulse-grid-attempts`. Records contain only attempt IDs, exam IDs, attempt-question IDs, selected attempt-option IDs, increasing client sequences, idempotent sync IDs and timestamps needed for synchronization. Each pending batch retains one immutable sync ID and answer snapshot until acknowledged, while newer sequences remain queued for the next batch. Tokens, access PINs, correct-answer flags, monitoring evidence and question-bank source identifiers are excluded. Higher answer sequences replace lower ones; acknowledged records are removed, and all local attempt data is deleted after safe finalization.

While an attempt is in progress, the client sends 15-second heartbeats and records only backend-supported browser events: tab hidden, window blur, fullscreen exit, copy, paste and network transitions. Events are UUID-addressed, held in memory for reconnect synchronization, and sent in idempotent batches; monitoring evidence is not persisted in IndexedDB. No webcam, face recognition or speculative developer-tools detection is claimed. Listeners and timers are removed when the runner unmounts or finalizes.

Institution staff results use the tenant-secured summary, candidate page, attempt review and UTF-8 CSV endpoints. Students receive their own result immediately after submission through `/api/v1/student/attempts/{attemptId}/result`. The backend currently has no student-scoped completed-results collection endpoint, so a persistent “My results” history is intentionally not exposed until that safe contract exists.
## Phase 5 operations

Phase 5 connects institution operations to the existing monitoring, audit and webhook contracts. Authorized institution staff can select a published examination, reconcile its REST monitoring dashboard with authenticated STOMP updates from `/ws`, and inspect paginated attempt events. The client supplies the current bearer access token only in the STOMP `CONNECT` header and subscribes to `/topic/exams/{examId}/monitoring`; it never sends application messages over STOMP. Connection loss triggers bounded REST reconciliation and subscriptions are disposed when the screen or session closes.

Institution administrators can review the immutable tenant audit trail using only backend-supported action, resource, actor UUID and time filters. They can also administer webhook subscriptions and delivery retries when the backend feature is enabled. Signing secrets are shown only in transient local dialog state after creation or rotation, can be copied only by an explicit user action, and are cleared when the dialog closes. They are never placed in storage, URLs, query caches, logs or notifications.
