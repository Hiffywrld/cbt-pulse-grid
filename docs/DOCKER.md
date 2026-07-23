# Docker Operations

CBT-Pulse Grid can run locally as a full stack with PostgreSQL, the Spring Boot backend and the React frontend:

```powershell
docker compose up --build
```

The frontend is available at `http://localhost:5173`. It proxies REST calls under `/api` and native WebSocket/STOMP traffic under `/ws` to the backend service.

## Services And Ports

- `postgres`: `postgres:17-alpine`, internal port `5432`, host port `${POSTGRES_PORT:-5433}`.
- `backend`: Spring Boot container, internal port `8080`, host port `${BACKEND_PORT:-8080}`.
- `frontend`: Nginx static frontend, internal port `80`, host port `${FRONTEND_PORT:-5173}`.

## Data Preservation

Compose keeps the existing named volume `postgres_data`. Do not remove this volume unless you intentionally want to delete the local database.

Useful commands:

```powershell
docker compose down
docker compose up --build
docker compose logs -f backend
docker compose exec postgres psql -U cbt_admin -d cbt_pulse_grid
```

`docker compose down` stops containers but preserves `postgres_data`. Avoid `docker compose down -v` unless data deletion is intended.

## Rebuilds

```powershell
docker compose build backend
docker compose build frontend
docker compose up -d
```

Flyway runs from the backend on startup and validates the schema through Hibernate.

## Port Conflicts

If Maven or Vite are already running locally, stop them or change `BACKEND_PORT` and `FRONTEND_PORT` in `.env`. If PostgreSQL is already listening on `5433`, change `POSTGRES_PORT` without changing the container's internal `5432`.

## Secrets

Local `.env` is ignored by Git. Production secrets must come from the deployment platform:

- database user and password
- `JWT_SECRET`
- Base64 `WEBHOOK_MASTER_KEY`
- bootstrap admin password, only for controlled initialization
