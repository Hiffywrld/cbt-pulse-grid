# CBT-Pulse Grid Microservices Defense Notes

## Positioning

CBT-Pulse Grid is defended as an incremental microservices migration. It has independently runnable services behind a gateway, service-level authorization, Docker packaging, Kubernetes manifests, and CI/CD image publishing. It intentionally keeps a shared PostgreSQL schema during this phase to avoid risky data destruction while proving runtime service separation.

## Why This Is Defensible

- The API Gateway is the single public entry point and preserves frontend routes.
- Identity, examination, and monitoring can be built, tested, started, stopped, and scaled separately.
- JWT validation happens inside each service, not only at the gateway.
- Tenant isolation and role checks remain in the owning service.
- PostgreSQL remains shared temporarily, with logical table ownership documented.
- The compatibility backend is explicitly isolated on a diagnostic port and is not the public API.

## Service Ownership Summary

Identity owns users, institutions, roles, refresh tokens, profile, passwords, avatars, and audit.

Examination owns academic content, exam lifecycle, candidate assignments, attempts, answer sync, scoring, results, CSV export, and absence reporting.

Monitoring owns heartbeat persistence, anti-cheat events, risk scoring, missed heartbeat detection, live STOMP updates, and webhook delivery.

## Security Arguments

The gateway routes requests but does not replace service authorization. Services validate JWTs locally, derive tenant identity from claims, reject cross-tenant access, and return safe JSON errors. Sensitive values such as passwords, JWTs, access PINs, webhook secrets, and database credentials are not logged or returned.

The self-suspension hardening closes an administrative self-lockout gap: `PATCH /api/v1/users/{id}/status` now rejects attempts by the authenticated principal to suspend or deactivate their own account with HTTP 409.

## Known Limitations

- The physical PostgreSQL database is still shared.
- Some extracted service source trees still carry compatibility classes from the modular-monolith codebase.
- Swagger/OpenAPI is still served by the compatibility backend route.
- The broad `/api/**` fallback remains until the team proves no additional backend-only route is required.
- Internal service-to-service HTTP clients are not yet the only way to read identity or institution data.

## Future Work

- Introduce service-owned schemas or databases.
- Remove cross-service foreign keys after data ownership is split.
- Replace direct shared-table reads with internal APIs or events.
- Remove compatibility fallback after endpoint parity is proven.
- Publish separate OpenAPI documents per service through the gateway.

## Defense Commands

```bash
docker compose ps
docker compose config
mvn -f identity-service/pom.xml test
mvn -f examination-service/pom.xml test
mvn -f monitoring-service/pom.xml test
mvn -f backend/pom.xml test
npm --prefix frontend run lint
npm --prefix frontend run test:run
npm --prefix frontend run build
kubectl apply --dry-run=client --validate=false -f k8s/
```
