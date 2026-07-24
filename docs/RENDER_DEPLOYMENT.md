# Render Deployment Guide

`render.yaml` defines a Render PostgreSQL database, a Docker-based backend web service and a static React frontend. It is a blueprint only; it does not mean the application has been deployed.

## Local Compose Versus Render

Local Compose runs PostgreSQL, backend and frontend on one Docker network. The frontend Nginx container proxies `/api` and `/ws` to the backend.

Render runs PostgreSQL as a managed database, the backend as a Docker web service, and the frontend as a static site. The frontend points directly to the deployed backend URL through build-time environment variables.

## Required Manual Environment Values

Backend:

- `JWT_SECRET`: secure random signing secret with at least 32 characters.
- `REST_CORS_ALLOWED_ORIGINS`: deployed frontend origin.
- `MONITORING_WEBSOCKET_ALLOWED_ORIGINS`: deployed frontend origin.
- `WEBHOOK_MASTER_KEY`: Base64-encoded 32-byte key when webhooks are enabled.
- `WEBHOOK_ENABLED`: `true` only after the master key and endpoint policy are ready.
- `BOOTSTRAP_ADMIN_ENABLED`: `true` only for controlled first-admin creation or recovery.
- `BOOTSTRAP_ADMIN_FORCE_RESET`: keep `false` normally; set to `true` only for a one-time password recovery of the configured bootstrap admin.
- `BOOTSTRAP_ADMIN_EMAIL`: initial administrator email, if bootstrap is enabled.
- `BOOTSTRAP_ADMIN_PASSWORD`: initial administrator password, if bootstrap is enabled.

Frontend:

- `VITE_API_BASE_URL`: deployed backend HTTPS origin.
- `VITE_WS_URL`: deployed backend WSS endpoint ending in `/ws`.

Database values are wired from the Render database references in `render.yaml`.

## Render Free Tier Notes

Free web services may spin down when idle, which can make the first request slow. Persistent production exam windows should be tested carefully against the selected Render plan before use.

## Smoke Tests After Deployment

- backend readiness: `/actuator/health/readiness`
- frontend homepage refresh: `/`
- React route refresh: `/login` and `/profile`
- login API: `POST /api/v1/auth/login`
- WebSocket/STOMP connection to `/ws`
- OpenAPI docs: `/v3/api-docs`
