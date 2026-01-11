#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export MAVEN_USER_HOME="${MAVEN_USER_HOME:-$ROOT_DIR/.m2}"

APP_PORT="${APP_PORT:-8080}"
APP_DB_PATH="${APP_DB_PATH:-$ROOT_DIR/backend/data/release.db}"
FRONTEND_PORT="${FRONTEND_PORT:-4173}"

export APP_PORT
export APP_DB_PATH
export VITE_BACKEND_TARGET="${VITE_BACKEND_TARGET:-http://localhost:${APP_PORT}}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "ERROR: '$1' not found." >&2
    exit 1
  fi
}

check_java17() {
  require_cmd java
  local major
  major="$(java -version 2>&1 | awk -F[\\\".] '/version/ {print $2; exit}')"
  if [[ -z "${major:-}" ]] || (( major < 17 )); then
    echo "ERROR: Java 17+ required, found: ${major:-unknown}" >&2
    java -version >&2 || true
    exit 1
  fi
}

resolve_mvn() {
  if command -v mvn >/dev/null 2>&1; then
    echo "mvn"
    return 0
  fi
  local mvnw="$ROOT_DIR/backend/mvnw"
  if [[ ! -x "$mvnw" ]]; then
    echo "ERROR: mvn not found and Maven Wrapper not executable: $mvnw" >&2
    exit 1
  fi
  echo "$mvnw"
}

wait_for_backend() {
  local base_url="http://localhost:${APP_PORT}"
  echo "Waiting for backend at ${base_url}/health ..."
  for _ in $(seq 1 60); do
    if command -v curl >/dev/null 2>&1; then
      if curl -fsS --max-time 1 "${base_url}/health" >/dev/null 2>&1; then
        echo "Backend is up."
        return 0
      fi
    fi
    sleep 1
  done
  echo "WARN: backend health check timed out; continuing." >&2
}

cleanup() {
  local rc=$?
  if [[ -n "${FRONTEND_PID:-}" ]] && kill -0 "$FRONTEND_PID" >/dev/null 2>&1; then
    kill "$FRONTEND_PID" >/dev/null 2>&1 || true
  fi
  if [[ -n "${BACKEND_PID:-}" ]] && kill -0 "$BACKEND_PID" >/dev/null 2>&1; then
    kill "$BACKEND_PID" >/dev/null 2>&1 || true
  fi
  exit "$rc"
}
trap cleanup INT TERM EXIT

check_java17
require_cmd node
require_cmd npm

MVN="$(resolve_mvn)"

mkdir -p "$(dirname "$APP_DB_PATH")"

echo "ROOT_DIR=$ROOT_DIR"
echo "APP_PORT=$APP_PORT"
echo "APP_DB_PATH=$APP_DB_PATH"
echo "FRONTEND_PORT=$FRONTEND_PORT"
echo "VITE_BACKEND_TARGET=$VITE_BACKEND_TARGET"
echo

if [[ "${APP_JWT_SECRET:-}" == "" ]]; then
  echo "WARN: APP_JWT_SECRET is not set; using Spring default placeholder." >&2
fi
if [[ "${APP_PASSWORD_PEPPER:-}" == "" ]]; then
  echo "WARN: APP_PASSWORD_PEPPER is not set; using default 'dev-pepper'." >&2
fi

echo "[1/4] Build backend (skip tests) ..."
"$MVN" -f "$ROOT_DIR/backend/pom.xml" -DskipTests package

jar_path="$(ls -1 "$ROOT_DIR"/backend/target/*SNAPSHOT*.jar 2>/dev/null | head -n 1 || true)"
if [[ -z "${jar_path:-}" ]]; then
  echo "ERROR: backend jar not found under backend/target (expected *SNAPSHOT*.jar)." >&2
  exit 1
fi

echo "[2/4] Build frontend ..."
if [[ ! -d "$ROOT_DIR/frontend/node_modules" ]]; then
  (cd "$ROOT_DIR/frontend" && npm install)
fi
(cd "$ROOT_DIR/frontend" && npm run build)

echo "[3/4] Start backend (jar) ..."
java -jar "$jar_path" &
BACKEND_PID=$!

wait_for_backend

echo "[4/4] Start frontend (vite preview) ..."
(
  cd "$ROOT_DIR/frontend"
  npm run preview -- --port "$FRONTEND_PORT"
) &
FRONTEND_PID=$!

echo
printf "Release servers started:\n  Frontend: http://localhost:%s\n  Backend:  http://localhost:%s\n\n" "$FRONTEND_PORT" "$APP_PORT"

wait "$FRONTEND_PID"
