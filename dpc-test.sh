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

if [ -n "$REPORT_COVERAGE" ]; then
  echo "┌──────────────────────────────────────┐"
  echo "│                                      │"
  echo "│      Running Tests and Coverage      │"
  echo "│                                      │"
  echo "└──────────────────────────────────────┘"
else
  echo "┌──────────────────────────────────────────┐"
  echo "│                                          │"
  echo "│              Running Tests....           │"
  echo "│           REPORT_COVERAGE not set        │"
  echo "│                                          │"
  echo "└──────────────────────────────────────────┘"
fi

# Build the application
docker compose -p start-v1-app up db --wait
mvn clean compile -Perror-prone -B -V -ntp
mvn package -Pci -ntp

# Format the test results
if [ -n "$REPORT_COVERAGE" ]; then
  mvn jacoco:report -ntp
fi

docker compose -p start-v1-app down

USE_BFD_MOCK=true docker compose -p start-v1-app up db attribution aggregation --wait

# Run the integration tests
USE_BFD_MOCK=true docker compose -p start-v1-app up --exit-code-from tests tests

docker compose -p start-v1-app down

echo "Starting api server for Postman tests"
# Start the API server
USE_BFD_MOCK=true AUTH_DISABLED=true docker compose -p start-v1-app up db attribution aggregation consent api --wait

echo "Starting Postman tests"
# Run the Postman tests
npm install
npm run test

# Wait for Jacoco to finish writing the output files
docker compose -p start-v1-app down -t 60

# Collect the coverage reports for the Docker integration tests
if [ -n "$REPORT_COVERAGE" ]; then
  mvn jacoco:report-integration -Pci -ntp
fi

echo "┌──────────────────────────────────────────┐"
echo "│                                          │"
echo "│             All Tests Complete           │"
echo "│                                          │"
echo "└──────────────────────────────────────────┘"
