#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-leo_ai_router}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"
OUT_FILE="${OUT_FILE:-deploy/mysql/init/01-schema-and-seed.sql}"

mkdir -p "$(dirname "$OUT_FILE")"

dump_common=(
  -h"$DB_HOST"
  -P"$DB_PORT"
  -u"$DB_USER"
  --default-character-set=utf8mb4
  --single-transaction
  --skip-lock-tables
  --set-gtid-purged=OFF
)

if [[ -n "$DB_PASSWORD" ]]; then
  dump_common+=("-p$DB_PASSWORD")
fi

{
  echo "CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  echo "USE \`$DB_NAME\`;"
  mysqldump "${dump_common[@]}" --no-data "$DB_NAME"
  mysqldump "${dump_common[@]}" --no-create-info "$DB_NAME" \
    user \
    model_provider \
    model \
    plugin_config \
    role \
    permission \
    role_permission \
    role_plan_limit \
    user_role \
    subscription_plan \
    point_package
} > "$OUT_FILE"

echo "Wrote $OUT_FILE"
