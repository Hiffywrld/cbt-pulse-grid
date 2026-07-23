# CBT-Pulse Grid Operational API

All endpoints below use the existing JWT bearer authentication and institution boundary checks. Unless stated otherwise, access is limited to `INSTITUTION_ADMIN`, `EXAMINER`, and `INVIGILATOR` users in the resource's institution. Students cannot use staff result or audit endpoints.

## Request correlation

Clients may send one canonical UUID in `X-Request-Id`. The server preserves a valid value or generates one when absent, and returns it in the response header. JSON API errors also include `requestId`. Malformed, overlong, or repeated request-id headers receive a generic `400` response. Request correlation never captures authorization headers, tokens, credentials, device identifiers, or request bodies.

## Staff results

- `GET /api/v1/results/exams/{examId}/summary` returns assigned, not-started, in-progress, submitted, auto-submitted, passed, and failed counts plus completed-attempt average/minimum/maximum percentage, pass rate, and total obtainable marks.
- `GET /api/v1/results/exams/{examId}/candidates` returns an assignment-based page, including candidates who have not started. Parameters are `search`, `status`, `passed`, `page`, and `size`; size is limited to 100.
- `GET /api/v1/results/attempts/{attemptId}` returns the candidate and saved result. A finalized attempt includes staff-only per-question answer review; an in-progress attempt never exposes correctness.
- `GET /api/v1/results/exams/{examId}/export.csv` accepts the same filters as candidate listing and returns UTF-8 CSV. Fields are quoted when required, and spreadsheet formula prefixes are neutralized.

Pass rate is `100 × passed finalized attempts / all finalized attempts`. Average, minimum, and maximum exclude not-started and in-progress candidates. Total obtainable marks is the sum of each exam pool rule's `questionCount × marksPerQuestion`.

## Audit history

`GET /api/v1/audit/events` is available only to an `INSTITUTION_ADMIN` for their own institution. It supports `action`, `resourceType`, `actorId`, inclusive `from` and `to` timestamps, `page`, and `size`; size is limited to 100. Events are ordered newest first. Audit entries are append-only and cannot be edited or deleted through the API.

## Health probes

- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`

Probe responses deliberately omit internal component details. `/actuator/info` remains authenticated.
