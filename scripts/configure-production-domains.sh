#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: sh scripts/configure-production-domains.sh <frontend-domain> <backend-domain>" >&2
  exit 1
fi

FRONTEND_DOMAIN="$1"
BACKEND_DOMAIN="$2"
ENV_FILE="${ENV_FILE:-deploy/.env.production}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "$ENV_FILE not found. Copy deploy/.env.production.example first." >&2
  exit 1
fi

tmp_file="$(mktemp)"
sed \
  -e "s#YOUR_FRONTEND_DOMAIN#${FRONTEND_DOMAIN}#g" \
  -e "s#YOUR_BACKEND_DOMAIN#${BACKEND_DOMAIN}#g" \
  "$ENV_FILE" > "$tmp_file"
mv "$tmp_file" "$ENV_FILE"

echo "Updated $ENV_FILE"
