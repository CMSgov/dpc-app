#!/bin/bash
set -Ee

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

# Create jacocoReport and make accessible for writing output from containers if we're running tests
if [ "$ENV" = 'local' ] || [ "$ENV" = 'github-ci' ]; then
  mkdir -p "${DIR}"/jacocoReport/dpc-api
  mkdir -p "${DIR}"/jacocoReport/dpc-attribution
  mkdir -p "${DIR}"/jacocoReport/dpc-aggregation
  chown -R nobody:nobody "${DIR}"/jacocoReport
fi

function _finally {
  # don't shut it down if running on ci
  if [ "$ENV" != 'github-ci' ]; then
    echo "SHUTTING EVERYTHING DOWN"
    docker compose -p client-integration-app down
    docker volume rm client-integration-app_pgdata16
  fi
}

trap _finally EXIT

# Build the application
docker compose -p client-integration-app up db --wait
mvn -T 1.5C clean compile -Perror-prone -B -V -ntp
mvn -T 1.5C package -Pci -ntp

echo "Starting api server for client integration tests"
USE_BFD_MOCK=true docker compose -p client-integration-app up api --wait

echo "Starting integration tests"
GOLDEN_MACAROON=$(curl -X POST http://localhost:9903/tasks/generate-token) \
SKIP_SIMPLE_COV=true \
docker compose -p client-integration-app -f docker-compose.yml -f docker-compose.portals.yml run --remove-orphans --entrypoint "bundle exec rspec --tag type:integration" dpc_client
