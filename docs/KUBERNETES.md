# Kubernetes Deployment Notes

The manifests in `k8s/` are deployment templates for a defense or cluster environment. They use GHCR image placeholders and Kubernetes Secrets for credentials.

Required files:

- `k8s/deployment.yaml`
- `k8s/service.yaml`
- `k8s/ingress.yaml`

Supporting files:

- `k8s/namespace.yaml`
- `k8s/configmap.yaml`
- `k8s/secret.example.yaml`
- `k8s/postgres.yaml`

## Preparation

Copy `k8s/secret.example.yaml` to a private secret manifest or create the secret with `kubectl create secret`. Replace only placeholders. Do not commit real secret values.

Update:

- GHCR image placeholders in `deployment.yaml`
- frontend hostname in `ingress.yaml`
- CORS and WebSocket origins in `configmap.yaml`

## Dry Run

```powershell
kubectl apply --dry-run=client -f k8s/namespace.yaml
kubectl apply --dry-run=client -f k8s/configmap.yaml
kubectl apply --dry-run=client -f k8s/secret.example.yaml
kubectl apply --dry-run=client -f k8s/postgres.yaml
kubectl apply --dry-run=client -f k8s/deployment.yaml
kubectl apply --dry-run=client -f k8s/service.yaml
kubectl apply --dry-run=client -f k8s/ingress.yaml
```

## Runtime Shape

Ingress routes `/api` and `/ws` to the backend and all other paths to the frontend. The Nginx ingress annotations keep WebSocket connections open for live monitoring.

PostgreSQL uses a PersistentVolumeClaim. Backups should be configured by the cluster operator or storage provider before production use.
