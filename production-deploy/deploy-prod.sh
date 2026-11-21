#!/usr/bin/env bash
# Wrapper entrypoint inside production-deploy/ delegating cleanly to ../scripts/deploy-prod.sh
set -euo pipefail

PACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$PACK_DIR/.." && pwd)"
TARGET_SCRIPT="$REPO_ROOT/scripts/deploy-prod.sh"

if [[ ! -f "$TARGET_SCRIPT" ]]; then
  printf '[ERR ] Cannot find %s. Run from inside the full repository.\n' "$TARGET_SCRIPT" >&2
  exit 1
fi

exec bash "$TARGET_SCRIPT" "$@"
