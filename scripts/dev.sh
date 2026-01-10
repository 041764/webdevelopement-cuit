#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export MAVEN_USER_HOME="${MAVEN_USER_HOME:-$ROOT_DIR/.m2}"

if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: java not found. Please install Java 17." >&2
  exit 1
fi

JAVA_MAJOR="$(java -version 2>&1 | awk -F[\\\".] '/version/ {print $2; exit}')"
if [[ -z "${JAVA_MAJOR:-}" ]] || (( JAVA_MAJOR < 17 )); then
  echo "ERROR: Java 17+ required, found: ${JAVA_MAJOR:-unknown}" >&2
  java -version >&2 || true
  exit 1
fi

MVN="mvn"
if ! command -v mvn >/dev/null 2>&1; then
  MVN="$ROOT_DIR/backend/mvnw"
  if [[ ! -x "$MVN" ]]; then
    echo "ERROR: mvn not found, and Maven Wrapper not executable: $MVN" >&2
    exit 1
  fi
fi

APP_PORT="${APP_PORT:-8080}"
export APP_PORT

export APP_DB_PATH="${APP_DB_PATH:-$ROOT_DIR/backend/data/dev.db}"
mkdir -p "$(dirname "$APP_DB_PATH")"

echo "ROOT_DIR=$ROOT_DIR"
echo "APP_PORT=$APP_PORT"
echo "APP_DB_PATH=$APP_DB_PATH"
echo "Starting backend (profile=dev)..."

exec "$MVN" -f "$ROOT_DIR/backend/pom.xml" -DskipTests spring-boot:run -Dspring-boot.run.profiles=dev
