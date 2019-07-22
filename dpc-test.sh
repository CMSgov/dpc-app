#!/bin/bash

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

mvn clean install -DskipTests=true -Dmaven.javadoc.skip=true -Perror-prone -B -V
mvn test -B -V
# Format the test results and copy to a new directory
mvn jacoco:report
mkdir reports

if [ -n "$REPORT_COVERAGE" ]; then
    for module in dpc-aggregation dpc-api dpc-attribution dpc-queue dpc-macaroons
    do
      JACOCO_SOURCE_PATH=./$module/src/main/java ./cc-test-reporter format-coverage ./$module/target/site/jacoco/jacoco.xml --input-type jacoco -o reports/codeclimate.unit.$module.json
    done
fi

ls reports
docker-compose down
docker-compose up -d --scale api=0
sleep 30

# Run the integration-test profile, which only runs a subset of the tests
mvn test -Pintegration-tests -pl dpc-api
mvn jacoco:report

if [ -n "$REPORT_COVERAGE" ]; then
    for module in dpc-api
    do
      JACOCO_SOURCE_PATH=./$module/src/main/java ./cc-test-reporter format-coverage ./$module/target/site/jacoco/jacoco.xml --input-type jacoco -o reports/codeclimate.integration.$module.json
    done
    ./cc-test-reporter sum-coverage reports/codeclimate.* -o coverage/codeclimate.json
    ./cc-test-reporter upload-coverage
fi

echo "┌──────────────────────────────────────────┐"
echo "│                                          │"
echo "│             All Tests Complete           │"
echo "│                                          │"
echo "└──────────────────────────────────────────┘"