#!/bin/bash
set -Ee

function _finally {
  echo "SHUTTING EVERYTHING DOWN"
  docker compose -p client-integration-app down
  docker volume rm client-integration-app_pgdata16
}

trap _finally EXIT

# Build the application
# mvn clean compile -Perror-prone -B -V -ntp -T 4 -DskipTests
# mvn package -Pci -ntp -T 4 -DskipTests

echo "Starting api server for client integration tests"
USE_BFD_MOCK=true docker compose -p client-integration-app up api --wait

echo "Starting integration tests"
GOLDEN_MACAROON=$(curl -X POST http://localhost:9903/tasks/generate-token) \
SKIP_SIMPLE_COV=true \
docker compose -p client-integration-app -f docker-compose.yml -f docker-compose.portals.yml run --remove-orphans --entrypoint "bundle exec rspec --tag type:integration" dpc_client
