#!/bin/bash
set -Ee

# Current working directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

username: jimson
password: 8731rkd*&hjfhsf

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
   echo "│                                          │"
   echo "│      Running Tests and Code Climate      │"
   echo "│                                          │"
   echo "└──────────────────────────────────────────┘"
else
    echo "┌──────────────────────────────────────────┐"
    echo "│                                          │"
    echo "│              Running Tests....           │"
    echo "│  REPORT_COVERAGE not set, not running CC │"
    echo "│                                          │"
    echo "└──────────────────────────────────────────┘"
fi

# Install Code Climate
if [ -n "$REPORT_COVERAGE" ]; then
  wget https://codeclimate.com/downloads/test-reporter/test-reporter-0.6.3-linux-amd64 -O ./cc-test-reporter
  chmod +x ./cc-test-reporter
  ./cc-test-reporter before-build
fi

# Build the application
docker-compose up start_core_dependencies
mvn clean compile -Perror-prone -B -V -ntp
mvn package -Pci -ntp

# Format the test results and copy to a new directory
if [ -n "$REPORT_COVERAGE" ]; then
    mvn jacoco:report -ntp
    mkdir -p reports

    for module in dpc-aggregation dpc-api dpc-attribution dpc-queue dpc-macaroons
    do
      JACOCO_SOURCE_PATH=./$module/src/main/java ./cc-test-reporter format-coverage ./$module/target/site/jacoco/jacoco.xml --input-type jacoco -o reports/codeclimate.unit.$module.json
    done
fi

docker-compose down
docker-compose up start_core_dependencies
docker-compose up start_api_dependencies

# Run the integration tests
docker-compose up --exit-code-from tests tests

docker-compose down
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

    for module in dpc-aggregation dpc-api dpc-attribution
    do
      JACOCO_SOURCE_PATH=./$module/src/main/java ./cc-test-reporter format-coverage ./$module/target/site/jacoco-it/jacoco.xml --input-type jacoco -o reports/codeclimate.integration.$module.json
    done

    ./cc-test-reporter sum-coverage reports/codeclimate.* -o coverage/codeclimate.json
    ./cc-test-reporter upload-coverage
fi

echo "┌──────────────────────────────────────────┐"
echo "│                                          │"
echo "│             All Tests Complete           │"
echo "│                                          │"
echo "└──────────────────────────────────────────┘"
