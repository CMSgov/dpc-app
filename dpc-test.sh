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

function _finally {
  docker-compose down
}
trap _finally EXIT

if [ -n "$REPORT_COVERAGE" ]; then
  echo "┌──────────────────────────────────────────┐"
  echo "│                                      │"
  echo "│      Running Tests and Coverage      │"
  echo "│                                      │"
  echo "└──────────────────────────────────────────┘"
else
  echo "┌──────────────────────────────────────────┐"
  echo "│                                          │"
  echo "│              Running Tests....           │"
  echo "│           REPORT_COVERAGE not set        │"
  echo "│                                          │"
  echo "└──────────────────────────────────────────┘"
fi

# Build the application
docker-compose up start_core_dependencies
mvn clean compile -Perror-prone -B -V -ntp
mvn package -Pci -ntp

# Format the test results
if [ -n "$REPORT_COVERAGE" ]; then
  mvn jacoco:report -ntp
fi

docker-compose down
docker volume rm dpc-app_pgdata
docker-compose up start_core_dependencies
docker-compose up start_api_dependencies

# Run the integration tests
docker-compose up --exit-code-from tests tests

docker-compose down
docker volume rm dpc-app_pgdata
docker-compose up start_core_dependencies
docker-compose up start_api_dependencies

# Start the API server
AUTH_DISABLED=true docker-compose up start_api start_consent

# Run the Postman tests
npm install
npm run test

# Wait for Jacoco to finish writing the output files
docker-compose down -t 60

# Collect the coverage reports for the Docker integration tests
if [ -n "$REPORT_COVERAGE" ]; then
  mvn jacoco:report-integration -Pci -ntp
fi

echo "┌──────────────────────────────────────────┐"
echo "│                                          │"
echo "│             All Tests Complete           │"
echo "│                                          │"
echo "└──────────────────────────────────────────┘"
