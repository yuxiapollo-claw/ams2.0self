# AMS 2.0 Task 11 Acceptance

## Scope

Task 11 only:

- Playwright smoke test validates `/login` can load.
- Deployment packaging assets exist and are wired in Compose.
- Acceptance verification is limited to this slice and excludes Task 12 persistence work.

## Implemented Assets

- `frontend/playwright.config.ts`
- `frontend/tests/e2e/login.spec.ts`
- `ops/backend/Dockerfile`
- `ops/frontend/Dockerfile`
- `ops/nginx/default.conf`
- `docker-compose.yml`
- `docs/test/ams2-acceptance.md`
- Supporting frontend wiring:
  - `frontend/package.json`
  - `frontend/package-lock.json`

## Verification Commands

Run from repository root:

```powershell
docker compose config
npm --prefix frontend run build
npm --prefix frontend run test:e2e:list
npm --prefix frontend run test:e2e:smoke
```

## Verification Results (2026-04-23)

- `docker compose config`: validated compose rendering for backend/frontend build services and port wiring.
- `npm --prefix frontend run build`: frontend build succeeds.
- `npm --prefix frontend run test:e2e:list`: lists smoke test from `tests/e2e` (`login.spec.ts`).
- `npm --prefix frontend run test:e2e:smoke`: passes (`1 passed`).

Browser installation history:

```powershell
npm --prefix frontend exec playwright install chromium
```

- Earlier attempts failed during browser download with repeated `ECONNRESET` (review feedback context).
- In this follow-up run, browser installation completed successfully and smoke execution is no longer blocked.

## Acceptance Notes

- Smoke assertion checks `AMS2.0` title and the visible login heading text `登录`.
- Frontend compose mapping exposes host port `5173` to container port `80` for the Nginx service.
- Nginx proxies `/api/` to backend service `backend:8080`.
