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
  validate-env                   Verify .env.production has no CHANGE_ME placeholders and passes Compose validation
  issue-ssl [email]              Issue Let's Encrypt TLS certs via containerized certbot (before starting nginx)
  test                           Run Express Jest + Go Signaling race tests inside ephemeral Docker containers
  up                             Validate env/certs and start all 7 production containers (mongo, redis, api, signaling, web, nginx, coturn)
  ensure-mongo-user              Idempotently create least-privilege MONGO_APP_USER on existing Mongo volumes
  create-admin <email> <phone> <password> [name]
                                 Create or promote an ADMIN account via oozox_api_prod container
  smoke                          Run all post-deployment health, TLS, auth-guard, and DB isolation checks
  backup                         Run AES-256-CBC PBKDF2 encrypted mongodump immediately
  install-backup-script          Install /opt/oozox/backup_mongo.sh and show Docker cron entries
  renew-ssl                      Renew TLS certificates via Dockerized certbot and reload Nginx
  update                         Zero-downtime rebuild & restart of api, signaling, and web containers
  status                         Show container status (`docker compose ps`)
  logs [service]                 Tail container logs (default: all services)

Environment overrides:
  ENV_FILE, COMPOSE_FILE, BACKUP_KEY_FILE, BACKUP_DIR, SERVER_PUBLIC_IP
EOF
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

  local mongo_root_pass mongo_app_pass db_enc_key redis_pass jwt_key coturn_key backup_pass
  mongo_root_pass="$(openssl rand -hex 24)"
  mongo_app_pass="$(openssl rand -hex 24)"
  db_enc_key="$(openssl rand -hex 32)"
  redis_pass="$(openssl rand -hex 24)"
  jwt_key="$(openssl rand -hex 48)"
  coturn_key="$(openssl rand -hex 32)"
  backup_pass="$(openssl rand -hex 32)"

  umask 077
  cat > "$ENV_FILE" <<EOF
# Auto-generated OOZOX Production Environment ($(date -u +"%Y-%m-%dT%H:%M:%SZ"))
SERVER_PUBLIC_IP=${public_ip}
TURN_REALM=turn.oozox.com

MONGO_ROOT_USER=oozox_root
MONGO_ROOT_PASSWORD=${mongo_root_pass}
MONGO_APP_USER=oozox_app
MONGO_APP_PASSWORD=${mongo_app_pass}
MONGO_DB_NAME=oozox_prod_db

DB_ENCRYPTION_KEY=${db_enc_key}

REDIS_PASSWORD=${redis_pass}

JWT_SECRET=${jwt_key}
JWT_EXPIRES_IN=7d

CLIENT_ORIGIN=https://oozox.com,https://www.oozox.com,https://admin.oozox.com
ALLOWED_ORIGINS=https://oozox.com,https://www.oozox.com,https://admin.oozox.com
API_RATE_LIMIT_MAX=500

COTURN_SECRET=${coturn_key}
TURN_URLS=turn:turn.oozox.com:3478?transport=udp,turn:turn.oozox.com:3478?transport=tcp,turns:turn.oozox.com:5349?transport=tcp
TURN_TTL=3600

VITE_API_BASE_URL=https://api.oozox.com
EOF
  chmod 600 "$ENV_FILE"
  log "Wrote $ENV_FILE (chmod 600) for SERVER_PUBLIC_IP=${public_ip}"

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
  if grep -q "CHANGE_ME" "$ENV_FILE"; then
    grep -n "CHANGE_ME" "$ENV_FILE" >&2
    die "Unreplaced CHANGE_ME placeholder(s) found in $ENV_FILE."
  fi
  dc config --quiet
  log "Environment & docker-compose.prod.yml validated OK."
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
  log "Running Express Backend Jest + Security tests inside node:20-alpine container..."
  docker run --rm -v "$REPO_DIR/backend:/app" -w /app node:20-alpine \
    sh -c "npm ci && npm test"

  log "Running Go WebRTC Signaling race tests inside golang:1.22-alpine container..."
  docker run --rm -v "$REPO_DIR/backend-signaling:/app" -w /app golang:1.22-alpine \
    sh -c "go mod tidy && go test -race -v ./..."

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
  local email="${1:-}"
  local phone="${2:-}"
  local password="${3:-}"
  local name="${4:-System Administrator}"

  [[ -n "$email" && -n "$phone" && "${#password}" -ge 10 ]] || \
    die "Usage: ./scripts/deploy-prod.sh create-admin <email> <phone> <password(10+ chars)> [name]"

  cmd_validate_env
  dc exec -T api node -e '
const mongoose = require("mongoose");
const User = require("./src/models/User");
(async () => {
  await mongoose.connect(process.env.MONGO_URI);
  const [email, phone, password, name] = process.argv.slice(1);
  const existing = await User.findOne({ $or: [{ email }, { phone }] });
  if (existing) {
    existing.role = "ADMIN";
    existing.password = password;
    existing.isVerified = true;
    await existing.save();
    console.log("Promoted user to ADMIN:", existing._id.toString());
  } else {
    const created = await User.create({ name, email, phone, password, role: "ADMIN", isVerified: true });
    console.log("Created ADMIN:", created._id.toString());
  }
  await mongoose.disconnect();
})();
' "$email" "$phone" "$password" "$name"
}

cmd_smoke() {
  require_cmd curl

  log "1/6 Checking Web Portal HTTPS (https://oozox.com)..."
  curl -fsSI https://oozox.com >/dev/null
  log "    Web Portal HTTPS OK"

  log "2/6 Checking Core API & MongoDB health (https://api.oozox.com/api/health)..."
  curl -fsS https://api.oozox.com/api/health
  printf '\n'

  log "3/6 Checking Signaling & Redis health (https://signaling.oozox.com/health)..."
  curl -fsS https://signaling.oozox.com/health
  printf '\n'

  log "4/6 Verifying dev /auth/token endpoint is disabled (expects HTTP 403)..."
  local code
  code="$(curl -s -o /dev/null -w '%{http_code}' -X POST https://signaling.oozox.com/auth/token -d '{"user_id":"test"}')"
  [[ "$code" == "403" ]] || die "Expected HTTP 403 from /auth/token in production, got HTTP $code"
  log "    Dev /auth/token blocked (HTTP 403) OK"

  log "5/6 Verifying MongoDB (27017) and Redis (6379) are NOT bound on public host ports..."
  if command -v ss >/dev/null 2>&1; then
    if ss -tulpn | grep -E ':(27017|6379)\s' >/dev/null 2>&1; then
      die "FAIL: Database port 27017 or 6379 is exposed on the host!"
    fi
  fi
  log "    Database network isolation OK"

  log "6/6 Verifying Redis requires authentication..."
  if docker exec oozox_redis_prod redis-cli ping 2>&1 | grep -q "NOAUTH"; then
    log "    Redis NOAUTH enforcement OK"
  else
    die "FAIL: Redis responded without authentication!"
  fi

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
  docker exec oozox_mongo_prod mongodump \
    --username="$MONGO_ROOT_USER" \
    --password="$MONGO_ROOT_PASSWORD" \
    --authenticationDatabase=admin \
    --db="${MONGO_DB_NAME:-oozox_prod_db}" \
    --archive --gzip \
  | openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000 \
    -pass env:BACKUP_ENCRYPTION_PASSPHRASE -out "$out_file"

  chmod 600 "$out_file"
  find "$BACKUP_DIR" -type f -name "*.archive.gz.enc" -mtime +30 -delete
  log "Encrypted backup created: $out_file"
}

cmd_install_backup_script() {
  local target_script="/opt/oozox/backup_mongo.sh"
  mkdir -p /opt/oozox "$BACKUP_DIR"
  cat > "$target_script" <<EOF
#!/usr/bin/env bash
set -euo pipefail
exec "$REPO_DIR/scripts/deploy-prod.sh" backup
EOF
  chmod 700 "$target_script"
  log "Installed $target_script (chmod 700)"
  cat <<EOF

Add these cron jobs via 'crontab -e':
0 3 * * * /opt/oozox/backup_mongo.sh >> /var/log/oozox_backup.log 2>&1
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
