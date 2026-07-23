# GitHub Actions And GHCR

Two workflows are defined:

- `.github/workflows/pull-request.yml` runs tests, builds the backend and frontend, and builds Docker images without publishing.
- `.github/workflows/main.yml` repeats the checks on pushes to `main` and publishes Docker images to GitHub Container Registry.

## Published Images

Pushes to `main` publish:

- `ghcr.io/<owner>/<repository>/cbt-pulse-grid-backend:<commit-sha>`
- `ghcr.io/<owner>/<repository>/cbt-pulse-grid-backend:latest`
- `ghcr.io/<owner>/<repository>/cbt-pulse-grid-frontend:<commit-sha>`
- `ghcr.io/<owner>/<repository>/cbt-pulse-grid-frontend:latest`

The workflow lowercases the GitHub owner and repository path before building image names.

## Permissions

The publishing workflow uses `GITHUB_TOKEN` with `packages: write` and does not require a personal access token.

## Troubleshooting

Check the workflow run logs in the repository's Actions tab. Common failures are:

- PostgreSQL service not becoming healthy before backend tests.
- Java 25 or Node 24 dependency resolution issues.
- frontend lint warnings promoted to failures by future rule changes.
- Docker build cache misses after dependency changes.

CI/CD is intentionally separated from public deployment. Images are published to GHCR, but no workflow deploys them to Render or Kubernetes.
