# CBT-Pulse Grid Microservices Runbook

## Local Runtime

Start the full local stack without deleting data:

```bash
docker compose up -d --build
```

Stop the stack while preserving the PostgreSQL named volume:

```bash
docker compose down
```

Do not use `docker compose down -v` unless the goal is to intentionally erase local database state.

## Ports

| Component | Local port |
| --- | ---: |
| API Gateway | 8080 |
| Identity Service | 8081 |
| Examination Service | 8082 |
| Monitoring Service | 8083 |
| Compatibility Backend | 18080 |
| Frontend | 5173 |
| PostgreSQL | 5433 |

## Health Checks

```bash
curl http://127.0.0.1:8080/healthz
curl http://127.0.0.1:8081/actuator/health/readiness
curl http://127.0.0.1:8082/actuator/health/readiness
curl http://127.0.0.1:8083/actuator/health/readiness
curl http://127.0.0.1:18080/actuator/health/readiness
curl http://127.0.0.1:5173/healthz
```

## Defense Smoke Commands

```bash
docker compose ps
docker compose config
git diff --check
```

Log in through the gateway, then use the returned access token for protected smoke tests. Do not paste complete tokens into reports.

```bash
curl -i http://127.0.0.1:8080/api/v1/auth/me
curl -i http://127.0.0.1:8080/api/v1/subjects?page=0\&size=5 -H "Authorization: Bearer <token>"
curl -i http://127.0.0.1:8080/api/v1/exams?page=0\&size=5 -H "Authorization: Bearer <token>"
curl -i http://127.0.0.1:8080/api/v1/webhooks/subscriptions?page=0\&size=5 -H "Authorization: Bearer <token>"
```

## Failure Diagnosis

Use container logs first:

```bash
docker logs --tail 200 cbt-pulse-grid-api-gateway
docker logs --tail 200 cbt-pulse-grid-identity-service
docker logs --tail 200 cbt-pulse-grid-examination-service
docker logs --tail 200 cbt-pulse-grid-monitoring-service
docker logs --tail 200 cbt-pulse-grid-backend
```

Check route ownership when an endpoint returns an unexpected service response:

```bash
docker exec cbt-pulse-grid-api-gateway nginx -T
```

Check database readiness:

```bash
docker exec cbt-pulse-grid-postgres pg_isready -U cbt_admin -d cbt_pulse_grid
```

## Request Correlation

The gateway forwards a caller-provided `X-Request-Id` unchanged. If the client omits it, the gateway omits it upstream so the Spring `RequestCorrelationFilter` generates one canonical UUID. Nginx `$request_id` is intentionally not used.

## WebSocket

The frontend connects to `/ws` and sends the JWT in the STOMP `CONNECT` header:

```text
Authorization: Bearer <access-token>
```

Staff subscribe to:

```text
/topic/exams/{examId}/monitoring
```

## Kubernetes

Apply manifests in a development cluster after replacing placeholder image names and hosts:

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.example.yaml
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml
```

The secret file is an example only and must be replaced with real cluster secrets outside Git.
