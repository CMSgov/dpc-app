#!/bin/bash

set -e

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

mvn clean compile -Perror-prone -B -V
mvn package -Pci

# Format the test results and copy to a new directory
if [ -n "$REPORT_COVERAGE" ]; then
    mvn jacoco:report
    mkdir -p reports

    for module in dpc-aggregation dpc-api dpc-attribution dpc-queue dpc-macaroons
    do
      JACOCO_SOURCE_PATH=./$module/src/main/java ./cc-test-reporter format-coverage ./$module/target/site/jacoco/jacoco.xml --input-type jacoco -o reports/codeclimate.unit.$module.json
    done
fi

docker-compose down
docker-compose up -d --scale api=0
sleep 60

# Run the integration tests
mvn test -Pintegration-tests -pl dpc-api -am

# Start the API server
docker-compose up -d
sleep 60

# Run the Postman tests
node_modules/.bin/newman run src/test/EndToEndRequestTest.postman_collection.json

# Wait for Jacoco to finish writing the output files
docker-compose down -t 60

# Collect the coverage reports for the Docker integration tests
if [ -n "$REPORT_COVERAGE" ]; then
    mvn jacoco:report-integration -Pci

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
