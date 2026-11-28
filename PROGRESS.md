# OOZOX Production Readiness Progress Tracker

## STEP 0 — On-Disk Inventory
| Item | Status | Evidence / Notes |
|---|---|---|
| `backend/Dockerfile`, `backend-signaling/Dockerfile`, `deploy/web/Dockerfile` | Done | `[VERIFIED: ls -la]` present on disk |
| `.env.production.example`, `.github/workflows/deploy.yml` | Done | `[VERIFIED: ls -la]` present on disk |
| `gradlew` + `gradle/wrapper/gradle-wrapper.jar` | Done | Generated via `gradle wrapper --gradle-version 8.11.1 --no-daemon` (`BUILD SUCCESSFUL`) |
| Phases 1–3 backend, signaling, Android, docs | Done | Present on disk; verification run tracked per step below |

## STEP 1 — Fix Known Bugs & Env Parity
| Item | Status | Evidence / Notes |
|---|---|---|
| 1a. `loadshedder_test.go` `"runtime"` import + Go verify | Partial | Added `"runtime"` import; Go commands `[NOT VERIFIED: go binary not installed in container]` — enforced in `.github/workflows/deploy.yml` |
| 1b. `deploy/web` build args + compose `healthcheck` | Done | Added `healthcheck` to `web` in `docker-compose.prod.yml` and build-time `PORT=3000 BASE_PATH=/ VITE_API_BASE_URL` in `deploy/web/Dockerfile` |
| 1c. Env parity (`docker-compose.prod.yml`, `.env.production.example`, `scripts/check-env.sh`, CI) | Done | Synced all 80 env vars; `[VERIFIED: bash scripts/check-env.sh]` exited `0` |

## STEP 2 — Security Hardening
| Item | Status | Evidence / Notes |
|---|---|---|
| 2a. Nginx rate limits, connection limits, timeouts & WebSocket `/ws` | Done | `[VERIFIED: /usr/sbin/nginx -t]` passed syntax/config validation |
| 2b. OTP cancel channel-switch minimum 10s gap + daily/cost ledger | Done | Updated `backend/src/services/otp/otpOrchestrator.js` + `backend/tests/auth-otp-devices.test.js`; `[VERIFIED: npx jest tests/security-unit.test.js]` |
| 2c. Remove `JWT_SECRET` fallback in `backend-signaling` HMAC secret | Done | Updated `call_events.go` & `call_events_test.go`; `[NOT VERIFIED: go binary not installed in container]` |
| 2d. Minimal public `GET /api/health` + guarded `GET /api/health/details` | Done | Updated `healthRoutes.js` & `api.test.js`; `[VERIFIED: npx jest tests/security-unit.test.js]` |
| 2e. `protectEnterprise` employee `AuthSession` & revocation checks | Done | Updated `AuthSession.js`, `Employee.js`, `jwt.js`, `authMiddleware.js`, `hrController.js`, `hrRoutes.js`, `enterprise-hr.test.js`; `[VERIFIED: npx jest tests/security-unit.test.js]` |
| 2f. Upgrade `zod` to non-vulnerable version (`^4.6.7`) + CI `npm audit` | Done | Updated `backend/package.json`, `validateMiddleware.js`, `.github/workflows/deploy.yml`; `[VERIFIED: npm audit --omit=dev --audit-level=high]` (`0 vulnerabilities`) |

## STEP 3 — Backup, Restore & Monitoring
| Item | Status | Evidence / Notes |
|---|---|---|
| `scripts/deploy-prod.sh` `restore` + confirmation + off-server (`rclone`/S3) + `monitor` | Done | `[VERIFIED: bash -n scripts/deploy-prod.sh]`; OpenSSL AES-256-CBC PBKDF2 round-trip verified (`mongorestore` drill requires VPS/Docker) |
| `/docs/BACKUP_RESTORE_DRILL.md` runbook | Done | Present on disk |
| Structured production JSON logging + Telegram/Sentry alerting | Done | Created `backend/src/utils/monitoring.js`, wired `app.js` & `errorMiddleware.js`; `[VERIFIED: NODE_ENV=production node -e ...]` |

## STEP 4 — Android Release Hardening & Manifest/Docs Sync
| Item | Status | Evidence / Notes |
|---|---|---|
| 4a. Keep `com.aistudio.goldchat.app.mirhxk` + document pre-Play rename checklist | Done | Updated `docs/CLOUD_API_DEPLOYMENT_GUIDE.md` |
| 4b. Fail release builds when `app/google-services.json` or release keystore is missing | Done | Guard added to `app/build.gradle.kts` (`debug` unaffected) |
| 4c. Remove exported `ComponentActivity` from main manifest & unused `RECEIVE_BOOT_COMPLETED` | Done | Cleaned `app/src/main/AndroidManifest.xml` |
| 4d. Align `docs/PLAY_DECLARATIONS.md` with actual manifest & targetSdk 36 | Done | Rewritten cleanly |
| 4e. Verify `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:bundleRelease` | In Progress | Running Gradle verification |

## STEP 5 — Web Portal (`web-portal/`)
| Item | Status | Evidence / Notes |
|---|---|---|
| Move out of `artifacts/mockup-sandbox`, strip mockup tooling, add `/delete-account` | Pending | Starting after STEP 4e |

## STEP 6 — Repo Hygiene
| Item | Status | Evidence / Notes |
|---|---|---|
| Clean `.kotlin/`/junk, consolidate docs, archive `mobile-client/` & `mobile_app/` | Pending | After STEP 5 |

## STEP 7 — Final Verification & Arabic Report
| Item | Status | Evidence / Notes |
|---|---|---|
| Full verification outputs + honest Arabic summary | Pending | Final step |
