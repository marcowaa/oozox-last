# 🚀 OOZOX Enterprise Production Suite
> **Platform:** OOZOX Financial & Invoicing System (CONNECT • THINK • DO)  
> **Production Stack:** Node.js 20 LTS + Express.js + MongoDB 7 + JWT + Docker + React Native / Android Compose

---

## 📑 English Summary & Architecture

The **OOZOX** platform provides a complete cloud backend and mobile integration layer engineered for high-security financial transactions, invoicing, and real-time ledger accounting.

### Key Capabilities:
- **Clean RESTful Architecture:** MVC layers (`models`, `controllers`, `routes`, `middlewares`, `services`).
- **Security & Encryption at Rest:** `bcrypt(12)` password hashing, `AES-256-GCM` transparent field-level encryption (`DB_ENCRYPTION_KEY`) for `Invoice.notes` and `Notification.message`, Helmet headers, strict CORS policies, Zod validation, and Rate Limiting.
- **Least-Privilege MongoDB:** Dedicated `MONGO_APP_USER` (`readWrite` on `oozox_prod_db` only) provisioned via `deploy/mongo/init-app-user.sh`.
- **Dockerized Infrastructure:** 100% Dockerized production stack (`docker-compose.prod.yml` in repo root) with isolated `internal: true` database network.

---

## 🛠️ 1. Full Production Stack vs Standalone Backend Docker

### Recommended: Full 100% Dockerized Stack (Repository Root)
From the repository root, use the turnkey deployment script and `docker-compose.prod.yml` (includes `mongo`, `redis`, `api`, `signaling`, `web`, `nginx` TLS, `coturn`, and `certbot`):

```bash
chmod +x scripts/deploy-prod.sh deploy/mongo/init-app-user.sh
./scripts/deploy-prod.sh init-env
./scripts/deploy-prod.sh issue-ssl admin@oozox.com
./scripts/deploy-prod.sh test
./scripts/deploy-prod.sh up
```

For full OpenCode instructions, firewall rules, and AES-256 encrypted backups, read `/OPENCODE_PRODUCTION_RUNBOOK.txt`.

### Standalone API + MongoDB (`backend/docker-compose.yml`)
```bash
cd backend
cp .env.example .env
chmod 600 .env
# Generate and set real values for MONGO_ROOT_PASSWORD, MONGO_APP_PASSWORD, DB_ENCRYPTION_KEY, JWT_SECRET
docker compose up -d --build
```

### Health & Logs:
- **Backend API:** `http://127.0.0.1:5000/api`
- **Health Check:** `http://127.0.0.1:5000/api/health`
- **Logs:** `docker compose logs -f api`

---

## 🧪 2. Automated Tests & Postman Verification

### Running Automated Test Suite (Jest + Supertest — `SEC-01`..`SEC-10`):
```bash
# Ephemeral Docker container (no host Node installation required):
docker run --rm -v "$(pwd)/backend:/app" -w /app node:20-alpine sh -c "npm ci && npm test"

# Or locally inside backend/:
cd backend && npm ci && npm test
```

### Importing Postman Collection:
1. Open **Postman**.
2. Click **Import** and select `backend/postman_collection.json`.
3. The collection automatically handles JWT auth tokens and dynamic variables.

---

## 🌐 3. Production Cloud Deployment Summary

All reverse proxy (`nginx`), TLS certificates (`certbot`), Web Portal (`web`), WebRTC Signaling (`signaling`), `coturn`, `mongo`, and `redis` run inside Docker containers defined in root `docker-compose.prod.yml`. Host Nginx or bare-metal MongoDB/Redis installations are **not** used and should be disabled if present (`systemctl disable --now nginx mongod redis-server`).

If using an external **MongoDB Atlas** cluster instead of the Dockerized `mongo` container, ensure `DB_ENCRYPTION_KEY` (64 hex chars via `openssl rand -hex 32`) and `MONGO_URI` are configured in `.env`.

---

## 📱 4. Android App Integration & Release Build (`app/`)

- API Base URL in `RetrofitClient.kt`: `https://api.oozox.com/api/`
- Signaling WebSocket URL in `WebRtcSignalingManager.kt`: `wss://signaling.oozox.com/ws`
- Application ID: `com.aistudio.goldchat.app.mirhxk`

### Building Android Unit Tests & Release AAB / APK (from repository root):
```bash
gradle :app:testDebugUnitTest
gradle :app:bundleRelease   # Outputs app/build/outputs/bundle/release/app-release.aab
gradle :app:assembleRelease # Outputs app/build/outputs/apk/release/app-release.apk
```

---

## 📄 API Endpoints Reference

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | Service & DB Health Check | Public |
| `POST` | `/api/auth/register` | Register new user / merchant | Public |
| `POST` | `/api/auth/login` | Authenticate & get JWT | Public |
| `GET` | `/api/auth/me` | Fetch active user profile | Protected (Bearer) |
| `GET` | `/api/auth/search?q=` | Search users for invoicing | Protected (Bearer) |
| `POST` | `/api/invoices` | Create invoice & notify recipient | Protected (Bearer) |
| `GET` | `/api/invoices` | List invoices (sent/received/filter) | Protected (Bearer) |
| `GET` | `/api/invoices/:id` | Get single invoice details | Protected (Bearer) |
| `PATCH` | `/api/invoices/:id/status`| Update status (PAID/OVERDUE) | Protected (Bearer) |
| `DELETE` | `/api/invoices/:id` | Cancel/Delete invoice | Protected (Bearer) |
| `GET` | `/api/notifications` | Get user notifications & unread count | Protected (Bearer) |
| `PATCH` | `/api/notifications/:id/read` | Mark single notification read | Protected (Bearer) |
| `PATCH` | `/api/notifications/read-all` | Mark all notifications read | Protected (Bearer) |

---
**Status:** ✅ Production Ready & Fully Tested.
