# CBT-Pulse Grid

CBT-Pulse Grid is a distributed institutional computer-based testing platform for secure concurrent examinations on offline-first local networks.

The `feature/microservices` branch runs the platform as an incremental microservices architecture:

- `api-gateway` on port `8080`
- `identity-service` on port `8081`
- `examination-service` on port `8082`
- `monitoring-service` on port `8083`
- compatibility `backend` on port `18080`
- `frontend` on port `5173`
- PostgreSQL on host port `5433`

## Run Locally

```bash
docker compose up -d --build
```

Stop without deleting the database volume:

```bash
docker compose down
```

## Documentation

- `docs/MICROSERVICES_ARCHITECTURE.md`
- `docs/MICROSERVICES_RUNBOOK.md`
- `docs/MICROSERVICES_DEFENSE.md`
- `docs/API.md`
- `docs/WEBHOOKS.md`
- `docs/KUBERNETES.md`

## Verification

```bash
mvn -f identity-service/pom.xml test
mvn -f examination-service/pom.xml test
mvn -f monitoring-service/pom.xml test
mvn -f backend/pom.xml test
npm --prefix frontend run lint
npm --prefix frontend run test:run
npm --prefix frontend run build
docker compose config
git diff --check
```

Do not commit real secrets. Use `.env.example`, `backend/.env.example`, Kubernetes secret examples, and deployment dashboards to configure environment variables.
