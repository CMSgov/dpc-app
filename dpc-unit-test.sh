#!/bin/sh
set -e

# Current working directory
DIR="$(cd "$(dirname "$0")" >/dev/null 2>&1 && pwd)"

# Configure the Maven log level
export MAVEN_OPTS=-Dorg.slf4j.simpleLogger.defaultLogLevel=info

# Include secure environment variables
if [ -f "${DIR}/ops/config/decrypted/local.env" ]; then
  . "${DIR}/ops/config/decrypted/local.env"
fi

_cleanup() {
  docker compose down
}
trap _cleanup EXIT

echo "┌──────────────────────────────────────────┐"
echo "│                                          │"
echo "│          Running Unit Tests...           │"
echo "│                                          │"
echo "└──────────────────────────────────────────┘"

mvn test -Punit-tests -ntp

echo "┌──────────────────────────────────────────┐"
echo "│                                          │"
echo "│           Unit Tests Complete            │"
echo "│                                          │"
echo "└──────────────────────────────────────────┘"
