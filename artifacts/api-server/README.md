# @workspace/api-server

Express 5 + TypeScript API server for the OOZOX workspace, providing health monitoring, Zod-validated schemas (`@workspace/api-zod`), Drizzle ORM integration (`@workspace/db`), structured Pino logging, and interactive OpenAPI / Swagger UI documentation.

---

## Prerequisites & Environment Variables

| Variable    | Required | Default | Description                                      |
| ----------- | -------- | ------- | ------------------------------------------------ |
| `PORT`      | **Yes**  | —       | TCP port for the HTTP server (must be > 0).      |
| `NODE_ENV`  | No       | —       | Set to `production` to disable `pino-pretty`.    |
| `LOG_LEVEL` | No       | `info`  | Pino log level (`trace`, `debug`, `info`, etc.). |

---

## Getting Started

From the monorepo root:

```bash
# Install dependencies
pnpm install

# Run in development mode (build + start)
PORT=3001 pnpm --filter @workspace/api-server run dev

# Type-check without emitting
pnpm --filter @workspace/api-server run typecheck

# Build production bundle (dist/index.mjs)
pnpm --filter @workspace/api-server run build

# Run the test suite (Jest + Supertest)
pnpm --filter @workspace/api-server run test
```

---

## Available API Endpoints

All routes are mounted under the `/api` base path.

### Health

| Method | Path           | Auth Required | Description                              |
| ------ | -------------- | ------------- | ---------------------------------------- |
| `GET`  | `/api/healthz` | No            | Returns server health status (`{ "status": "ok" }`). |

### API Documentation (Swagger / OpenAPI)

| Method | Path                | Auth Required | Description                                      |
| ------ | ------------------- | ------------- | ------------------------------------------------ |
| `GET`  | `/api/docs`         | No            | Interactive Swagger UI generated from `@workspace/api-zod` schemas. |
| `GET`  | `/api/openapi.json` | No            | Raw OpenAPI 3.0.3 JSON specification.            |

---

## Authentication Requirements

The API specification defines a **JWT Bearer** security scheme (`bearerAuth`):

```http
Authorization: Bearer <your-jwt-token>
```

- **Public endpoints:**
  - `GET /api/healthz` — Publicly accessible for liveness/readiness probes.
  - `GET /api/docs` and `GET /api/openapi.json` — Publicly accessible documentation endpoints.
- **Protected endpoints (future / domain routes):**
  - Endpoints that require user identity or tenant isolation expect a valid JWT in the `Authorization: Bearer <token>` header.
  - Note: Pino HTTP request logging automatically redacts `req.headers.authorization`, `req.headers.cookie`, and `res.headers['set-cookie']` so credentials are never written to logs.

---

## Shared Schemas (`@workspace/api-zod`)

OpenAPI component schemas are automatically generated from the Zod definitions in `@workspace/api-zod`:

- `HealthCheckResponse` — `{ status: string }`
- `RegisterRequest` — User registration payload (`name`, `email`, `phone`, `password`, `role`)
- `LoginRequest` — Authentication payload (`emailOrPhone`, `password`)
- `InvoiceItem` — Line item schema (`name`, `quantity`, `unitPrice`, `karat`)
- `CreateInvoiceRequest` — Invoice creation payload (`receiverId`, `items`, `taxAmount`, `discount`, `paymentMethod`, `dueDate`, `notes`)
- `UpdateInvoiceStatusRequest` — Invoice status update (`PENDING`, `PAID`, `OVERDUE`, `CANCELLED`)
