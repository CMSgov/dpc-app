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

function _finally {
  # don't shut it down if running on ci
  if [ "$ENV" != 'github-ci' ]; then
    echo "SHUTTING EVERYTHING DOWN"
    docker compose -p start-v1-app down
    docker volume rm start-v1-app_pgdata16
  fi
}

trap _finally EXIT

docker compose -f ./docker-compose.base.yml build base
mvn clean compile -Perror-prone -B -V -ntp -T 4 -DskipTests
mvn package -Pci -ntp -T 4 -DskipTests

echo "Starting api server for end-to-end tests"
USE_BFD_MOCK=true docker compose -p start-v1-app up aggregation api --wait

echo "Starting end-to-end tests"
docker run --add-host=host.docker.internal:host-gateway --rm -v $(pwd)/dpc-load-testing:/src --env-file $(pwd)/ops/config/decrypted/local.env -e ENVIRONMENT=local -e TEST_TYPE=all-apis -i grafana/k6 run /src/ci-app.js

# Wait for Jacoco to finish writing the output files
docker compose -p start-v1-app down -t 60
