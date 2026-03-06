#!/bin/bash
set -Ee

echo "┌-------------------───────────────────────┐"
echo "│                                          │"
echo "│ Running API Client Gem Integration Tests |"
echo "│                                          │"
echo "└------------─────────-------──────────────┘"

# Current working directory
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"

# Configure the Maven log level
export MAVEN_OPTS=-Dorg.slf4j.simpleLogger.defaultLogLevel=info

# Include secure environment variables
set -o allexport
[[ -f ${DIR}/ops/config/decrypted/local.env ]] && source ${DIR}/ops/config/decrypted/local.env
set +o allexport

# Remove jacocoReport directory
if [ -d "${DIR}/jacocoReport" ]; then
    rm -r "${DIR}/jacocoReport"
fi

function _finally {
  # don't shut it down if running on ci
  if [ "$ENV" != 'github-ci' ]; then
    echo "SHUTTING EVERYTHING DOWN"
    docker compose -p api-client-integration-app down
    docker volume rm api-client-integration-app_pgdata16
  fi
}

trap _finally EXIT

# Build the application
mvn -T 1.5C clean compile -Perror-prone -B -V -ntp -DskipTests
mvn -T 1.5C package -Pci -ntp -DskipTests

echo "Starting api server for end-to-end tests"
USE_BFD_MOCK=true docker compose -p api-client-integration-app up api --wait

echo "Starting integration tests"
GOLDEN_MACAROON=$(curl -X POST http://localhost:9903/tasks/generate-token) \
SKIP_SIMPLE_COV=true \
docker compose -p api-client-integration-app \
-f docker-compose.yml -f docker-compose.api-client.yml \
run --remove-orphans \
--entrypoint "bundle exec rspec --order defined --tag type:integration" \
dpc_api_client

echo "┌───────────-------──────────-------------──┐"
echo "│                                           │"
echo "│ API Client Gem Integration Tests Complete |"
echo "│                                           │"
echo "└────────────────────--------------------───┘"
