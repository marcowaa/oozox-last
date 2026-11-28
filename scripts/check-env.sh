#!/usr/bin/env bash
# Verifies that every environment variable read by backend/src (process.env.*)
# and backend-signaling (os.Getenv / os.LookupEnv) is explicitly declared in
# docker-compose.prod.yml and documented in .env.production.example.
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${REPO_DIR}/docker-compose.prod.yml"
ENV_EXAMPLE="${REPO_DIR}/.env.production.example"

# Variables synthesized/wired inside docker-compose.prod.yml from component vars
# (e.g. MONGO_URI, REDIS_URL, PORT, APP_ENV, AUTH_TOKEN_ENABLED, BACKEND_INTERNAL_URL)
# are present directly as keys in docker-compose.prod.yml service environment blocks,
# so they don't also need separate top-level placeholders in .env.production.example.
COMPOSE_ONLY_KEYS="^(PORT|MONGO_URI|REDIS_URL|REDIS_HOST|REDIS_ADDR|APP_ENV|AUTH_TOKEN_ENABLED|BACKEND_INTERNAL_URL)$"

node_vars="$(grep -rhoE 'process\.env\.[A-Z0-9_]+' "${REPO_DIR}/backend/src" | sed 's/process\.env\.//' | sort -u)"
go_vars="$(grep -rhoE 'os\.(Getenv|LookupEnv)\("[A-Z0-9_]+"' "${REPO_DIR}/backend-signaling" | sed -E 's/.*os\.(Getenv|LookupEnv)\("([A-Z0-9_]+)".*/\2/' | sort -u)"
all_vars="$(printf '%s\n%s\n' "${node_vars}" "${go_vars}" | grep -v '^$' | sort -u)"

missing_compose=0
missing_example=0
checked=0

for var in ${all_vars}; do
  checked=$((checked + 1))
  if ! grep -Eq "^[[:space:]]*${var}:" "${COMPOSE_FILE}"; then
    printf '[ERR ] Env var used in code is missing from docker-compose.prod.yml: %s\n' "${var}" >&2
    missing_compose=1
  fi
  if [[ ! "${var}" =~ ${COMPOSE_ONLY_KEYS} ]]; then
    if ! grep -Eq "^#?[[:space:]]*${var}=" "${ENV_EXAMPLE}"; then
      printf '[ERR ] Env var used in code is missing from .env.production.example: %s\n' "${var}" >&2
      missing_example=1
    fi
  fi
done

if [[ "${missing_compose}" -ne 0 || "${missing_example}" -ne 0 ]]; then
  exit 1
fi

printf '[OK  ] All %d env vars from backend/src and backend-signaling match docker-compose.prod.yml and .env.production.example.\n' "${checked}"
