#!/usr/bin/env bash
# ==============================================================================
# OOZOX Platform — 100% Dockerized Production Deployment & Operations Script
# Usage: ./scripts/deploy-prod.sh <command> [args]
# ==============================================================================
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-$REPO_DIR/docker-compose.prod.yml}"
ENV_FILE="${ENV_FILE:-$REPO_DIR/.env.production}"
BACKUP_KEY_FILE="${BACKUP_KEY_FILE:-/opt/oozox/.backup.key}"
BACKUP_DIR="${BACKUP_DIR:-/opt/oozox/backups}"

dc() {
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" "$@"
}

log()  { printf '[INFO] %s\n' "$*"; }
warn() { printf '[WARN] %s\n' "$*" >&2; }
die()  { printf '[ERR ] %s\n' "$*" >&2; exit 1; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Required command '$1' is not installed."
}

usage() {
  cat <<'EOF'
OOZOX Production Deployment CLI (100% Dockerized Stack)

Commands:
  check-prereqs                  Verify Docker, Compose v2, OpenSSL, and host port readiness
  init-env [--force]             Generate .env.production and /opt/oozox/.backup.key with random secrets
  validate-env                   Verify .env.production secrets, OTP provider readiness, and Compose validation
  issue-ssl [email]              Issue Let's Encrypt TLS certs via containerized certbot (before starting nginx)
  test                           Run Express Jest (bookworm-slim) + Go Signaling race tests in Docker
  up                             Validate env/certs and start all 7 production containers (mongo, redis, api, signaling, web, nginx, coturn)
  ensure-mongo-user              Idempotently create least-privilege MONGO_APP_USER on existing Mongo volumes
  create-admin <phone> [name]    Create or promote an OTP-only ADMIN account via oozox_api_prod container
  smoke                          Run all post-deployment health, TLS, auth-guard, Prometheus firewall & DB isolation checks
  backup                         Run AES-256-CBC PBKDF2 encrypted mongodump + optional off-server copy (rclone/S3)
  restore <backup-file>          Decrypt and restore backup with confirmation prompt (--target-db <db> for drill, --yes to confirm)
  monitor                        Check web/API/signaling uptime & disk usage and send Telegram alert on failure
  install-backup-script          Install /opt/oozox/backup_mongo.sh + monitor script and show cron entries
  renew-ssl                      Renew TLS certificates via Dockerized certbot and reload Nginx
  update                         Zero-downtime rebuild & restart of api, signaling, and web containers
  status                         Show container status (`docker compose ps`)
  logs [service]                 Tail container logs (default: all services)

Environment overrides:
  ENV_FILE, COMPOSE_FILE, BACKUP_KEY_FILE, BACKUP_DIR, SERVER_PUBLIC_IP, BACKUP_RCLONE_REMOTE, BACKUP_S3_URI
EOF
}

send_telegram_alert() {
  local title="$1"
  local details="${2:-}"
  if [[ -z "${ALERT_TELEGRAM_BOT_TOKEN:-}" || -z "${ALERT_TELEGRAM_CHAT_ID:-}" ]]; then
    return 0
  fi
  command -v curl >/dev/null 2>&1 || return 0
  local payload
  payload="[OOZOX OPS ALERT] ${title}"$'\n'"${details}"
  curl -fsS --max-time 8 -X POST "https://api.telegram.org/bot${ALERT_TELEGRAM_BOT_TOKEN}/sendMessage" \
    --data-urlencode "chat_id=${ALERT_TELEGRAM_CHAT_ID}" \
    --data-urlencode "text=${payload}" \
    --data-urlencode "disable_web_page_preview=true" >/dev/null 2>&1 || true
}

cmd_check_prereqs() {
  require_cmd docker
  require_cmd openssl
  require_cmd curl

  docker compose version >/dev/null 2>&1 || die "Docker Compose v2 ('docker compose') is required."
  log "Docker & Compose v2 OK: $(docker compose version)"

  if command -v ss >/dev/null 2>&1; then
    if ss -tulpn 2>/dev/null | grep -E ':(80|443)\s' | grep -v 'docker-proxy' >/dev/null 2>&1; then
      warn "Another host process is listening on port 80 or 443. Stop host nginx/apache2 before starting Docker Nginx/Certbot."
    fi
  fi
}

cmd_init_env() {
  local force="${1:-}"
  require_cmd openssl

  if [[ -f "$ENV_FILE" && "$force" != "--force" ]]; then
    die "$ENV_FILE already exists. Pass --force to overwrite (CAUTION: overwriting DB_ENCRYPTION_KEY breaks decryption of existing encrypted DB fields!)."
  fi

  local public_ip="${SERVER_PUBLIC_IP:-}"
  if [[ -z "$public_ip" ]]; then
    public_ip="$(curl -4 -fsS --max-time 8 https://ifconfig.me || true)"
  fi
  [[ -n "$public_ip" ]] || die "Could not detect SERVER_PUBLIC_IP. Export SERVER_PUBLIC_IP=<YOUR_VPS_IP> and retry."

  local mongo_root_pass mongo_app_pass db_enc_key redis_pass jwt_key coturn_key signaling_hmac_key backup_pass
  mongo_root_pass="$(openssl rand -hex 24)"
  mongo_app_pass="$(openssl rand -hex 24)"
  db_enc_key="$(openssl rand -hex 32)"
  redis_pass="$(openssl rand -hex 24)"
  jwt_key="$(openssl rand -hex 48)"
  coturn_key="$(openssl rand -hex 32)"
  signaling_hmac_key="$(openssl rand -hex 32)"
  backup_pass="$(openssl rand -hex 32)"

  umask 077
  cat > "$ENV_FILE" <<EOF
# Auto-generated OOZOX Production Environment ($(date -u +"%Y-%m-%dT%H:%M:%SZ"))
NODE_ENV=production
TRUST_PROXY_HOPS=1
SERVER_PUBLIC_IP=${public_ip}
TURN_REALM=turn.oozox.com

MONGO_ROOT_USER=oozox_root
MONGO_ROOT_PASSWORD=${mongo_root_pass}
MONGO_APP_USER=oozox_app
MONGO_APP_PASSWORD=${mongo_app_pass}
MONGO_DB_NAME=oozox_prod_db

DB_ENCRYPTION_KEY=${db_enc_key}

REDIS_PASSWORD=${redis_pass}
REDIS_DB=0

JWT_SECRET=${jwt_key}
JWT_EXPIRES_IN=7d
PRE_AUTH_EXPIRES_IN=15m
DEVICE_TRUST_DAYS=30
REFRESH_TOKEN_DAYS=30
SENSITIVE_STEP_UP_SECONDS=900

CLIENT_ORIGIN=https://oozox.com,https://www.oozox.com,https://admin.oozox.com
ALLOWED_ORIGINS=https://oozox.com,https://www.oozox.com,https://admin.oozox.com
API_RATE_LIMIT_MAX=500
LOGIN_RATE_LIMIT_MAX=10
OTP_IP_RATE_LIMIT_MAX=25
AI_IP_RATE_LIMIT_PER_MIN=20

# Mandatory: configure at least one OTP provider (TELEGRAM_GATEWAY_TOKEN OR all 3 TWILIO_* keys)
OTP_CHANNEL_ORDER=TELEGRAM,WHATSAPP,SMS
TELEGRAM_GATEWAY_TOKEN=CHANGE_ME_TELEGRAM_GATEWAY_TOKEN
TELEGRAM_GATEWAY_BASE_URL=https://gatewayapi.telegram.org
TELEGRAM_GATEWAY_CALLBACK_URL=https://api.oozox.com/api/auth/webhooks/telegram-gateway
TELEGRAM_GATEWAY_COST_USD=0.01
TWILIO_ACCOUNT_SID=CHANGE_ME_TWILIO_ACCOUNT_SID
TWILIO_AUTH_TOKEN=CHANGE_ME_TWILIO_AUTH_TOKEN
TWILIO_VERIFY_SERVICE_SID=CHANGE_ME_TWILIO_VERIFY_SERVICE_SID
TWILIO_SMS_COST_USD=0.05
TWILIO_WHATSAPP_COST_USD=0.025
OTP_ALLOWED_COUNTRIES=+20,+966,+971,+965,+974,+973,+968,+962
OTP_RESEND_COOLDOWN_SECONDS=60
OTP_DAILY_MAX_PER_PHONE=5
OTP_TTL_SECONDS=300
OTP_MAX_ATTEMPTS=5
OTP_LOCK_MINUTES=15
OTP_MONTHLY_BUDGET_USD=150

COTURN_SECRET=${coturn_key}
TURN_URLS=turn:turn.oozox.com:3478?transport=udp,turn:turn.oozox.com:3478?transport=tcp,turns:turn.oozox.com:5349?transport=tcp
TURN_TTL=3600
COTURN_METRICS_URL=http://host.docker.internal:9641/metrics
INTERNAL_SIGNALING_HMAC_SECRET=${signaling_hmac_key}
REQUIRE_CALL_AUTH_SIGNATURE=true
RTC_MONTHLY_BUDGET_USD=100
RTC_ALERT_THRESHOLD_1_PCT=50
RTC_ALERT_THRESHOLD_2_PCT=80
RTC_COST_PER_MINUTE_USD=0.004
RTC_TURN_COST_PER_GB_USD=0.09
RTC_DAILY_USER_LIMIT_MINUTES=0
RTC_MAX_CALL_DURATION_SECONDS=14400
LOAD_SHED_ENABLED=true
LOAD_SHED_MAX_GOROUTINES=10000
LOAD_SHED_GOROUTINES_ENTER_THRESHOLD=10000
LOAD_SHED_GOROUTINES_EXIT_THRESHOLD=8000
LOAD_SHED_MAX_SYSTEM_LOAD=0
LOAD_SHED_SYSTEM_LOAD_ENTER_THRESHOLD=0
LOAD_SHED_SYSTEM_LOAD_EXIT_THRESHOLD=0
LOAD_SHED_SAMPLE_INTERVAL_MS=1000
LOAD_SHED_SUSTAINED_TICKS=2
LOAD_SHED_RECOVERY_TICKS=3
MAINTENANCE_INTERVAL_SECONDS=30
NODE_INACTIVITY_THRESHOLD_SECONDS=90

# Recommended: Server-side Gemini AI proxy & Firebase Cloud Messaging
GEMINI_API_KEY=
GEMINI_MODEL=gemini-2.5-flash
GEMINI_USER_DAILY_LIMIT=30
GEMINI_USER_MONTHLY_LIMIT=300
GEMINI_MONTHLY_BUDGET_USD=50
GEMINI_TIMEOUT_MS=30000
FIREBASE_SERVICE_ACCOUNT_JSON=
FIREBASE_SERVICE_ACCOUNT_PATH=

MAX_CLOUD_BACKUPS_PER_USER=10
TRANSFER_TTL_MINUTES=15
BACKUP_RCLONE_REMOTE=
BACKUP_S3_URI=

LOG_LEVEL=info
SENTRY_DSN=
ALERT_TELEGRAM_BOT_TOKEN=
ALERT_TELEGRAM_CHAT_ID=

VITE_API_BASE_URL=https://api.oozox.com
WEB_PORTAL_DIR=web-portal
WEB_WORKSPACE_FILTER=@oozox/web-portal
EOF
  chmod 600 "$ENV_FILE"
  log "Wrote $ENV_FILE (chmod 600) for SERVER_PUBLIC_IP=${public_ip}"
  warn "ACTION REQUIRED: Edit $ENV_FILE and configure TELEGRAM_GATEWAY_TOKEN or TWILIO_* credentials before running validate-env/up."

  local key_dir
  key_dir="$(dirname "$BACKUP_KEY_FILE")"
  if mkdir -p "$key_dir" 2>/dev/null && [[ -w "$key_dir" ]]; then
    if [[ ! -f "$BACKUP_KEY_FILE" || "$force" == "--force" ]]; then
      printf 'BACKUP_ENCRYPTION_PASSPHRASE=%s\n' "$backup_pass" > "$BACKUP_KEY_FILE"
      chmod 600 "$BACKUP_KEY_FILE"
      log "Wrote $BACKUP_KEY_FILE (chmod 600)"
    else
      log "Kept existing $BACKUP_KEY_FILE"
    fi
  else
    warn "Cannot write $BACKUP_KEY_FILE (create $key_dir or run with sudo). Generated backup passphrase saved nowhere else — create $BACKUP_KEY_FILE manually before running backups."
  fi

  log "CRITICAL: Store an offline copy of DB_ENCRYPTION_KEY from $ENV_FILE and $BACKUP_KEY_FILE safely."
}

cmd_validate_env() {
  [[ -f "$ENV_FILE" ]] || die "Missing $ENV_FILE. Run: ./scripts/deploy-prod.sh init-env"

  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a

  local telegram_ok=0 twilio_ok=0
  if [[ -n "${TELEGRAM_GATEWAY_TOKEN:-}" && "${TELEGRAM_GATEWAY_TOKEN}" != *"CHANGE_ME"* ]]; then
    telegram_ok=1
  fi
  if [[ -n "${TWILIO_ACCOUNT_SID:-}" && "${TWILIO_ACCOUNT_SID}" != *"CHANGE_ME"* && \
        -n "${TWILIO_AUTH_TOKEN:-}" && "${TWILIO_AUTH_TOKEN}" != *"CHANGE_ME"* && \
        -n "${TWILIO_VERIFY_SERVICE_SID:-}" && "${TWILIO_VERIFY_SERVICE_SID}" != *"CHANGE_ME"* ]]; then
    twilio_ok=1
  fi

  if [[ "$telegram_ok" -eq 0 && "$twilio_ok" -eq 0 ]]; then
    die "No OTP provider configured in $ENV_FILE! Set TELEGRAM_GATEWAY_TOKEN or ALL THREE TWILIO_ACCOUNT_SID/TWILIO_AUTH_TOKEN/TWILIO_VERIFY_SERVICE_SID."
  fi

  # Check remaining CHANGE_ME placeholders (ignore inactive optional OTP provider keys if the other provider is active)
  local unresolved_lines
  unresolved_lines="$(grep -n "CHANGE_ME" "$ENV_FILE" || true)"
  if [[ -n "$unresolved_lines" ]]; then
    local filtered=""
    while IFS= read -r line; do
      [[ -z "$line" ]] && continue
      if [[ "$telegram_ok" -eq 1 && "$line" =~ ^[0-9]+:(TWILIO_ACCOUNT_SID|TWILIO_AUTH_TOKEN|TWILIO_VERIFY_SERVICE_SID)= ]]; then
        continue
      fi
      if [[ "$twilio_ok" -eq 1 && "$line" =~ ^[0-9]+:(TELEGRAM_GATEWAY_TOKEN|TELEGRAM_GATEWAY_CALLBACK_URL)= ]]; then
        continue
      fi
      filtered+="$line"$'\n'
    done <<< "$unresolved_lines"

    if [[ -n "$filtered" ]]; then
      printf '%s' "$filtered" >&2
      die "Unreplaced CHANGE_ME placeholder(s) found in $ENV_FILE."
    fi
  fi

  if [[ -z "${FIREBASE_SERVICE_ACCOUNT_JSON:-}" ]]; then
    warn "FIREBASE_SERVICE_ACCOUNT_JSON is empty: FCM push notifications and incoming-call wakeups will be disabled."
  fi
  if [[ -z "${GEMINI_API_KEY:-}" ]]; then
    warn "GEMINI_API_KEY is empty: Server-side AI assistant and contextual translation endpoints will be unavailable."
  fi

  dc config --quiet
  log "Environment & docker-compose.prod.yml validated OK (OTP: telegram=${telegram_ok}, twilio=${twilio_ok})."
}

cmd_issue_ssl() {
  local admin_email="${1:-admin@oozox.com}"
  cmd_check_prereqs
  cmd_validate_env

  mkdir -p /etc/letsencrypt /var/www/certbot

  # Ensure nginx container is not holding port 80 while certbot runs standalone
  dc stop nginx >/dev/null 2>&1 || true

  local -a cert_groups=(
    "-d oozox.com -d www.oozox.com -d admin.oozox.com"
    "-d api.oozox.com"
    "-d signaling.oozox.com"
    "-d turn.oozox.com"
  )

  for domains in "${cert_groups[@]}"; do
    log "Issuing certificate for: $domains"
    # shellcheck disable=SC2086
    dc run --rm --service-ports certbot certonly \
      --standalone --non-interactive --agree-tos --keep-until-expiring \
      --email "$admin_email" $domains
  done

  log "All Let's Encrypt certificates issued under /etc/letsencrypt/live/"
}

cmd_test() {
  cmd_check_prereqs
  # Note: mongodb-memory-server does not run reliably on Alpine/musl and requires outbound
  # internet access on first run to download the mongod binary inside Debian bookworm-slim.
  log "Running Express Backend Jest + Security tests inside node:20-bookworm-slim container (requires internet for mongod binary download)..."
  docker run --rm -v "$REPO_DIR/backend:/app" -w /app node:20-bookworm-slim \
    sh -c "npm ci && npm test"

  log "Running Go WebRTC Signaling race tests inside golang:1.22 container..."
  docker run --rm -v "$REPO_DIR/backend-signaling:/app" -w /app golang:1.22 \
    sh -c "go mod tidy && go vet ./... && go test -race -v ./..."

  log "All containerized backend & signaling tests PASSED."
}

cmd_up() {
  cmd_check_prereqs
  cmd_validate_env

  chmod +x "$REPO_DIR/deploy/mongo/init-app-user.sh"

  local missing_certs=0
  for host in oozox.com api.oozox.com signaling.oozox.com turn.oozox.com; do
    if [[ ! -f "/etc/letsencrypt/live/${host}/fullchain.pem" ]]; then
      warn "Missing TLS certificate: /etc/letsencrypt/live/${host}/fullchain.pem"
      missing_certs=1
    fi
  done
  if [[ "$missing_certs" -ne 0 ]]; then
    die "TLS certificates are required before starting Nginx and Coturn. Run: ./scripts/deploy-prod.sh issue-ssl <your-email>"
  fi

  log "Building and starting all 7 Dockerized production services..."
  dc up -d --build
  dc ps
  log "Stack started. Run './scripts/deploy-prod.sh smoke' after containers report healthy."
}

cmd_ensure_mongo_user() {
  cmd_validate_env
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a

  docker exec -i oozox_mongo_prod mongosh --quiet \
    --username "$MONGO_ROOT_USER" \
    --password "$MONGO_ROOT_PASSWORD" \
    --authenticationDatabase admin <<EOF
const dbName = "${MONGO_DB_NAME:-oozox_prod_db}";
const appUser = "${MONGO_APP_USER:-oozox_app}";
const appPass = "${MONGO_APP_PASSWORD}";
const target = db.getSiblingDB(dbName);
if (!target.getUser(appUser)) {
  target.createUser({
    user: appUser,
    pwd: appPass,
    roles: [{ role: "readWrite", db: dbName }]
  });
  print("Created MONGO_APP_USER: " + appUser);
} else {
  target.changeUserPassword(appUser, appPass);
  print("Synchronized password for existing MONGO_APP_USER: " + appUser);
}
EOF
}

cmd_create_admin() {
  local phone="${1:-}"
  local name="${2:-System Administrator}"

  [[ -n "$phone" ]] || \
    die "Usage: ./scripts/deploy-prod.sh create-admin <phone_e164> [name] (OTP-only authentication)"

  cmd_validate_env
  dc exec -T api node -e '
const mongoose = require("mongoose");
const User = require("./src/models/User");
const { normalizePhoneE164 } = require("./src/utils/phoneUtils");
(async () => {
  await mongoose.connect(process.env.MONGO_URI);
  const [rawPhone, name] = process.argv.slice(1);
  const phoneE164 = normalizePhoneE164(rawPhone);
  if (!phoneE164) {
    console.error("Invalid E.164 phone number:", rawPhone);
    process.exit(1);
  }
  const existing = await User.findOne({ $or: [{ phoneE164 }, { phone: phoneE164 }, { phone: rawPhone }] });
  if (existing) {
    existing.role = "ADMIN";
    existing.isVerified = true;
    existing.phone = phoneE164;
    existing.phoneE164 = phoneE164;
    if (name) existing.name = name;
    await existing.save();
    console.log("Promoted OTP-only user to ADMIN:", existing._id.toString(), phoneE164);
  } else {
    const created = await User.create({
      name: name || "System Administrator",
      phone: phoneE164,
      phoneE164,
      role: "ADMIN",
      isVerified: true,
      phoneVerifiedAt: new Date()
    });
    console.log("Created OTP-only ADMIN:", created._id.toString(), phoneE164);
  }
  await mongoose.disconnect();
})();
' "$phone" "$name"
}

cmd_smoke() {
  require_cmd curl
  cmd_validate_env
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a

  log "1/7 Checking Web Portal HTTPS (https://oozox.com)..."
  curl -fsSI https://oozox.com >/dev/null
  log "    Web Portal HTTPS OK"

  log "2/7 Checking Core API & MongoDB health (https://api.oozox.com/api/health)..."
  curl -fsS https://api.oozox.com/api/health
  printf '\n'

  log "3/7 Checking Signaling & Redis health (https://signaling.oozox.com/health)..."
  curl -fsS https://signaling.oozox.com/health
  printf '\n'

  log "4/7 Verifying dev /auth/token endpoint is disabled (expects HTTP 404 or 403)..."
  local code
  code="$(curl -s -o /dev/null -w '%{http_code}' -X POST https://signaling.oozox.com/auth/token -d '{"user_id":"test"}')"
  [[ "$code" == "403" || "$code" == "404" ]] || die "Expected HTTP 403/404 from /auth/token in production, got HTTP $code"
  log "    Dev /auth/token blocked (HTTP $code) OK"

  log "5/7 Verifying MongoDB (27017) and Redis (6379) are NOT bound on public host ports..."
  if command -v ss >/dev/null 2>&1; then
    if ss -tulpn | grep -E ':(27017|6379)\s' >/dev/null 2>&1; then
      die "FAIL: Database port 27017 or 6379 is exposed on the host!"
    fi
  fi
  log "    Database network isolation OK"

  log "6/7 Verifying Redis requires authentication..."
  if docker exec oozox_redis_prod redis-cli ping 2>&1 | grep -q "NOAUTH"; then
    log "    Redis NOAUTH enforcement OK"
  else
    die "FAIL: Redis responded without authentication!"
  fi

  log "7/7 Verifying Coturn Prometheus port 9641 is NOT reachable from public IP (${SERVER_PUBLIC_IP})..."
  if curl -s --max-time 3 "http://${SERVER_PUBLIC_IP}:9641/metrics" >/dev/null 2>&1; then
    die "FAIL: Coturn Prometheus exporter port 9641 is exposed on ${SERVER_PUBLIC_IP}! Firewall TCP 9641 immediately."
  fi
  log "    Coturn Prometheus port 9641 blocked from public IP OK"

  log "All smoke checks PASSED."
}

cmd_backup() {
  require_cmd openssl
  cmd_validate_env
  [[ -f "$BACKUP_KEY_FILE" ]] || die "Missing $BACKUP_KEY_FILE containing BACKUP_ENCRYPTION_PASSPHRASE=..."

  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  # shellcheck disable=SC1090
  source "$BACKUP_KEY_FILE"
  set +a

  [[ -n "${BACKUP_ENCRYPTION_PASSPHRASE:-}" ]] || die "BACKUP_ENCRYPTION_PASSPHRASE is empty in $BACKUP_KEY_FILE"

  mkdir -p "$BACKUP_DIR"
  local ts out_file
  ts="$(date +"%Y%m%d_%H%M%S")"
  out_file="$BACKUP_DIR/oozox_mongo_${ts}.archive.gz.enc"

  export BACKUP_ENCRYPTION_PASSPHRASE
  if ! docker exec oozox_mongo_prod mongodump \
      --username="$MONGO_ROOT_USER" \
      --password="$MONGO_ROOT_PASSWORD" \
      --authenticationDatabase=admin \
      --db="${MONGO_DB_NAME:-oozox_prod_db}" \
      --archive --gzip \
    | openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000 \
      -pass env:BACKUP_ENCRYPTION_PASSPHRASE -out "$out_file"; then
    send_telegram_alert "MongoDB Backup Failed" "Host: $(hostname) Target: $out_file"
    die "MongoDB encrypted backup failed!"
  fi

  chmod 600 "$out_file"
  log "Encrypted backup created locally: $out_file"

  if [[ -n "${BACKUP_RCLONE_REMOTE:-}" ]]; then
    require_cmd rclone
    if ! rclone copy "$out_file" "$BACKUP_RCLONE_REMOTE"; then
      send_telegram_alert "Off-Server Backup Copy Failed (rclone)" "File: $(basename "$out_file") Remote: $BACKUP_RCLONE_REMOTE"
      die "Off-server rclone upload failed!"
    fi
    log "Off-server copy uploaded via rclone -> $BACKUP_RCLONE_REMOTE"
  elif [[ -n "${BACKUP_S3_URI:-}" ]]; then
    require_cmd aws
    local s3_target="${BACKUP_S3_URI%/}/$(basename "$out_file")"
    if ! aws s3 cp "$out_file" "$s3_target" --sse AES256; then
      send_telegram_alert "Off-Server Backup Copy Failed (S3)" "File: $(basename "$out_file") Target: $s3_target"
      die "Off-server S3 upload failed!"
    fi
    log "Off-server copy uploaded via S3 -> $s3_target"
  else
    warn "No off-server backup target set (BACKUP_RCLONE_REMOTE or BACKUP_S3_URI). Configure one before going live!"
  fi

  find "$BACKUP_DIR" -type f -name "*.archive.gz.enc" -mtime +30 -delete
}

cmd_restore() {
  require_cmd openssl
  local backup_file="${1:-}"
  shift || true

  local target_db=""
  local auto_confirm=0
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --target-db)
        target_db="${2:-}"
        shift 2
        ;;
      --yes)
        auto_confirm=1
        shift
        ;;
      *)
        die "Unknown option for restore: $1"
        ;;
    esac
  done

  [[ -n "$backup_file" ]] || die "Usage: ./scripts/deploy-prod.sh restore <backup-file> [--target-db <db>] [--yes]"
  [[ -f "$backup_file" ]] || die "Backup file not found: $backup_file"
  [[ -f "$BACKUP_KEY_FILE" ]] || die "Missing $BACKUP_KEY_FILE containing BACKUP_ENCRYPTION_PASSPHRASE=..."

  cmd_validate_env
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  # shellcheck disable=SC1090
  source "$BACKUP_KEY_FILE"
  set +a

  [[ -n "${BACKUP_ENCRYPTION_PASSPHRASE:-}" ]] || die "BACKUP_ENCRYPTION_PASSPHRASE is empty in $BACKUP_KEY_FILE"

  local source_db="${MONGO_DB_NAME:-oozox_prod_db}"
  local restore_db="${target_db:-$source_db}"

  if [[ "$auto_confirm" -ne 1 ]]; then
    warn "DANGER: Restoring '$backup_file' will DROP & REPLACE collections in MongoDB database '${restore_db}'."
    local confirm=""
    read -r -p "Type RESTORE to confirm restoring into '${restore_db}': " confirm
    [[ "$confirm" == "RESTORE" ]] || die "Restore aborted by operator."
  fi

  local -a ns_args=()
  if [[ "$restore_db" != "$source_db" ]]; then
    ns_args=(--nsFrom "${source_db}.*" --nsTo "${restore_db}.*")
  fi

  export BACKUP_ENCRYPTION_PASSPHRASE
  if ! openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 \
      -pass env:BACKUP_ENCRYPTION_PASSPHRASE -in "$backup_file" \
    | docker exec -i oozox_mongo_prod mongorestore \
      --username="$MONGO_ROOT_USER" \
      --password="$MONGO_ROOT_PASSWORD" \
      --authenticationDatabase=admin \
      --drop \
      "${ns_args[@]}" \
      --archive --gzip; then
    send_telegram_alert "MongoDB Restore Failed" "Backup: $(basename "$backup_file") Target DB: ${restore_db}"
    die "MongoDB restore failed!"
  fi

  log "Restore completed successfully into MongoDB database '${restore_db}'."
  send_telegram_alert "MongoDB Restore Completed" "Backup: $(basename "$backup_file") -> DB: ${restore_db}"
}

cmd_monitor() {
  require_cmd curl
  if [[ -f "$ENV_FILE" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
  fi

  local failures=""
  local api_url="${MONITOR_API_URL:-https://api.oozox.com/api/health}"
  local sig_url="${MONITOR_SIGNALING_URL:-https://signaling.oozox.com/health}"
  local web_url="${MONITOR_WEB_URL:-https://oozox.com/}"

  if ! curl -fsS --max-time 8 "$api_url" | grep -q '"HEALTHY"'; then
    failures+="- Core API health check failed ($api_url)"$'\n'
  fi
  if ! curl -fsS --max-time 8 "$sig_url" | grep -q '"HEALTHY"'; then
    failures+="- Signaling health check failed ($sig_url)"$'\n'
  fi
  if ! curl -fsSI --max-time 8 "$web_url" >/dev/null 2>&1; then
    failures+="- Web Portal HTTPS check failed ($web_url)"$'\n'
  fi

  local disk_pct
  disk_pct="$(df -P "$REPO_DIR" | awk 'NR==2 {gsub(/%/,"",$5); print $5}')"
  if [[ -n "$disk_pct" && "$disk_pct" -ge 85 ]]; then
    failures+="- Disk usage critical: ${disk_pct}% on ${REPO_DIR}"$'\n'
  fi

  if [[ -n "$failures" ]]; then
    send_telegram_alert "Production Uptime / Resource Warning ($(hostname))" "$failures"
    printf '%s' "$failures" >&2
    exit 1
  fi
  log "Uptime monitor OK (API, Signaling, Web, Disk ${disk_pct:-0}%)."
}

cmd_install_backup_script() {
  local target_script="/opt/oozox/backup_mongo.sh"
  local monitor_script="/opt/oozox/monitor_health.sh"
  mkdir -p /opt/oozox "$BACKUP_DIR"
  cat > "$target_script" <<EOF
#!/usr/bin/env bash
set -euo pipefail
exec "$REPO_DIR/scripts/deploy-prod.sh" backup
EOF
  cat > "$monitor_script" <<EOF
#!/usr/bin/env bash
set -euo pipefail
exec "$REPO_DIR/scripts/deploy-prod.sh" monitor
EOF
  chmod 700 "$target_script" "$monitor_script"
  log "Installed $target_script and $monitor_script (chmod 700)"
  cat <<EOF

Add these cron jobs via 'crontab -e':
0 3 * * * /opt/oozox/backup_mongo.sh >> /var/log/oozox_backup.log 2>&1
*/5 * * * * /opt/oozox/monitor_health.sh >> /var/log/oozox_monitor.log 2>&1
0 4 * * 1 cd $REPO_DIR && ./scripts/deploy-prod.sh renew-ssl >> /var/log/oozox_certbot.log 2>&1
EOF
}

cmd_renew_ssl() {
  cmd_validate_env
  dc run --rm certbot renew --webroot -w /var/www/certbot
  docker exec oozox_nginx_prod nginx -s reload
  log "TLS certificate renewal check complete and Nginx reloaded."
}

cmd_update() {
  cmd_validate_env
  log "Rebuilding api, signaling, and web containers for rolling update..."
  dc build api signaling web
  dc up -d --no-deps api signaling web
  dc ps
}

main() {
  local subcmd="${1:-help}"
  shift || true

  case "$subcmd" in
    check-prereqs)          cmd_check_prereqs "$@" ;;
    init-env)               cmd_init_env "$@" ;;
    validate-env)           cmd_validate_env "$@" ;;
    issue-ssl)              cmd_issue_ssl "$@" ;;
    test)                   cmd_test "$@" ;;
    up)                     cmd_up "$@" ;;
    ensure-mongo-user)      cmd_ensure_mongo_user "$@" ;;
    create-admin)           cmd_create_admin "$@" ;;
    smoke)                  cmd_smoke "$@" ;;
    backup)                 cmd_backup "$@" ;;
    restore)                cmd_restore "$@" ;;
    monitor)                cmd_monitor "$@" ;;
    install-backup-script)  cmd_install_backup_script "$@" ;;
    renew-ssl)              cmd_renew_ssl "$@" ;;
    update)                 cmd_update "$@" ;;
    status)                 dc ps "$@" ;;
    logs)                   dc logs --tail=100 -f "$@" ;;
    help|-h|--help)         usage ;;
    *)                      die "Unknown command: $subcmd (run './scripts/deploy-prod.sh help')" ;;
  esac
}

main "$@"
