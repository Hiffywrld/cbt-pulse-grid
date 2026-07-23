# Production Readiness Checklist

## Secrets

- Database credentials are stored in the platform secret manager.
- `JWT_SECRET` is unique, private and long enough.
- `WEBHOOK_MASTER_KEY` is Base64 encoded and 32 bytes before encoding.
- Bootstrap admin credentials are disabled or rotated after first setup.
- No `.env`, tokens, passwords or API keys are committed.

## Configuration

- Backend honors the deployment `PORT` environment variable.
- CORS and WebSocket origins match the deployed frontend origin.
- Frontend `VITE_API_BASE_URL` points to the public backend HTTPS origin or same-origin proxy.
- Frontend `VITE_WS_URL` uses `wss://` in production.
- Actuator readiness and liveness endpoints are reachable.

## Persistence

- Flyway migrations run at backend startup.
- Hibernate schema validation remains enabled.
- PostgreSQL storage is persistent.
- Backup and restore procedures are documented by the operator.

## Verification

- Maven full test suite passes.
- Spring Modulith verification passes.
- frontend lint, tests and production build pass.
- Docker images build.
- `docker compose config` passes.
- Kubernetes manifests pass client dry-run.
- `git diff --check` is clean.
