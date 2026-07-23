# Troubleshooting

## Backend Is Not Ready

Check:

- `docker compose logs -f backend`
- PostgreSQL health with `docker compose ps`
- database variables: `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`
- Flyway migration errors
- Hibernate schema validation errors

## Frontend Loads But API Calls Fail

In local Compose, the frontend should call same-origin `/api/v1/...`. Nginx proxies `/api` to the backend. In Vite development, `VITE_API_BASE_URL` should usually be `http://localhost:8080`.

## WebSocket Monitoring Does Not Connect

Check:

- `VITE_WS_URL`
- allowed WebSocket origin
- STOMP `Authorization: Bearer <access-token>` connect header
- proxy support for `Upgrade` and long read timeouts

## Login Fails After Deployment

Confirm CORS origin, database connectivity and JWT configuration. Do not log tokens or passwords while debugging.

## Local Data Was Lost

The normal command `docker compose down` preserves `postgres_data`. Data loss usually means the named volume was explicitly removed with `-v` or through Docker Desktop volume cleanup.
