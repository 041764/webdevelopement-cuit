#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export MAVEN_USER_HOME="${MAVEN_USER_HOME:-$ROOT_DIR/.m2}"

APP_PORT="${APP_PORT:-8080}"
APP_BASE_URL="${APP_BASE_URL:-http://localhost:${APP_PORT}}"
export APP_DB_PATH="${APP_DB_PATH:-$ROOT_DIR/backend/data/dev.db}"

mkdir -p "$ROOT_DIR/docs"
mkdir -p "$(dirname "$APP_DB_PATH")"

echo "ROOT_DIR=$ROOT_DIR"
echo "APP_BASE_URL=$APP_BASE_URL"
echo "APP_DB_PATH=$APP_DB_PATH"
echo

openapi_ok="no"
openapi_out="$ROOT_DIR/docs/openapi.json"

echo "[1/2] Export OpenAPI from running service: $APP_BASE_URL/v3/api-docs"
if command -v curl >/dev/null 2>&1; then
  if curl -fsS --max-time 3 "$APP_BASE_URL/v3/api-docs" -o "$openapi_out"; then
    if [[ -s "$openapi_out" ]] && head -c 1 "$openapi_out" | grep -q '{'; then
      openapi_ok="yes"
      echo "  OK -> $openapi_out"
    else
      echo "  WARN: response saved but not valid JSON object head; kept file: $openapi_out" >&2
    fi
  else
    echo "  SKIP: service not reachable. Start it via: ./scripts/dev.sh" >&2
  fi
else
  echo "  SKIP: curl not found; cannot export. Install curl or export manually." >&2
fi

echo
echo "[2/2] Flyway migrate (SQLite) -> $APP_DB_PATH"
(
  cd "$ROOT_DIR"
  MVN="mvn"
  if ! command -v mvn >/dev/null 2>&1; then
    MVN="$ROOT_DIR/backend/mvnw"
  fi
  attempt=1
  max_attempts=3
  while :; do
    if "$MVN" -f backend/pom.xml -DskipTests \
      -Dflyway.url="jdbc:sqlite:$APP_DB_PATH" \
      -Dflyway.user="" \
      -Dflyway.password="" \
      flyway:migrate; then
      break
    fi

    rc=$?
    if (( attempt >= max_attempts )); then
      echo "  ERROR: Flyway migrate failed after ${max_attempts} attempts." >&2
      echo "  ERROR: Dependency download/network may be transient (e.g. TLS bad_record_mac). Retry or switch network." >&2
      exit "$rc"
    fi

    echo "  WARN: Flyway migrate failed (attempt ${attempt}/${max_attempts}); retrying in 3s..." >&2
    sleep 3
    ((attempt++))
  done
)
echo "  OK"

echo
echo "Summary:"
echo "  DB: $APP_DB_PATH"
if [[ "$openapi_ok" == "yes" ]]; then
  echo "  OpenAPI JSON: $openapi_out"
else
  echo "  OpenAPI JSON: (not exported; service may be down) -> $openapi_out"
fi
