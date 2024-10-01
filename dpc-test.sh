#!/bin/bash
PROJECT_NAME="start-v1-app"

IS_AWS_EC2=$(./ops/scripts/is_aws_ec2.sh)

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
  docker compose -p $PROJECT_NAME down
  docker volume rm "$PROJECT_NAME"_pgdata14
  echo "^^^^^^^^^^^^^^^"
  echo "└└└└└└└└└└└└└└└-------- this volume has been removed!"
}

trap _finally EXIT

if [ -n "$REPORT_COVERAGE" ]; then
  echo "┌──────────────────────────────────────┐"
  echo "│                                      │"
  echo "│      Running Tests and Coverage      │"
  if [ "$IS_AWS_EC2" == "yes" ]; then
    echo "│              (AWS EC2)               │"
  else
    echo "│                                      │"
  fi
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
echo "┌──────────────────────────────────────┐"
echo "│                                      │"
echo "│          Application Build           │"
echo "│                                      │"
echo "└──────────────────────────────────────┘"
DOCKER_PROJECT_NAME=$PROJECT_NAME make start-db
mvn clean compile -Perror-prone -B -V -ntp
mvn package -Pci -ntp

# Format the test results
if [ -n "$REPORT_COVERAGE" ]; then
  echo "┌──────────────────────────────────────┐"
  echo "│                                      │"
  echo "│       Formatting Test Results        │"
  echo "│                                      │"
  echo "└──────────────────────────────────────┘"
  mvn jacoco:report -ntp
fi

DOCKER_PROJECT_NAME=$PROJECT_NAME make down-dpc
docker volume rm "$PROJECT_NAME"_pgdata14
echo "^^^^^^^^^^^^^^^"
echo "└└└└└└└└└└└└└└└-------- this volume has been removed!"

# Run the integration tests
echo "┌──────────────────────────────────────┐"
echo "│                                      │"
echo "│     Running integration tests...     │"
echo "│                                      │"
echo "└──────────────────────────────────────┘"
docker compose -p $PROJECT_NAME up --exit-code-from tests tests

DOCKER_PROJECT_NAME=$PROJECT_NAME make down-dpc
docker volume rm "$PROJECT_NAME"_pgdata14
echo "^^^^^^^^^^^^^^^"
echo "└└└└└└└└└└└└└└└-------- this volume has been removed!"

# Run the system tests
echo "┌──────────────────────────────────────┐"
echo "│                                      │"
echo "│        Running system tests...       │"
echo "│                                      │"
echo "└──────────────────────────────────────┘"

# Start the API server
AUTH_DISABLED=true DOCKER_PROJECT_NAME=$PROJECT_NAME make start-api

# Run the Postman tests
npm install
npm run test

# Wait for Jacoco to finish writing the output files
docker compose -p $PROJECT_NAME down -t 60

# Collect the coverage reports for the Docker integration tests
if [ -n "$REPORT_COVERAGE" ]; then
  mvn jacoco:report-integration -Pci -ntp
fi

echo "┌──────────────────────────────────────────┐"
echo "│                                          │"
echo "│             All Tests Complete           │"
echo "│                                          │"
echo "└──────────────────────────────────────────┘"
