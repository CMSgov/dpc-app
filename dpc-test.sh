echo "┌──────────────────────────────────────────┐"
echo "│                                          │"
echo "│              Running Tests....           │"
echo "│                                          │"
echo "└──────────────────────────────────────────┘"

mvn clean install -DskipTests=true -Dmaven.javadoc.skip=true -Perror-prone -B -V
mvn test -B -V
# Format the test results and copy to a new directory
mvn jacoco:report
mkdir reports

for module in dpc-aggregation dpc-api dpc-attribution dpc-queue dpc-macaroons
do
  JACOCO_SOURCE_PATH=./$module/src/main/java ./cc-test-reporter format-coverage ./$module/target/site/jacoco/jacoco.xml --input-type jacoco -o reports/codeclimate.unit.$module.json
done

ls reports
docker-compose down
docker-compose up -d --scale api=0
sleep 30

# Run the integration-test profile, which only runs a subset of the tests
mvn test -Pintegration-tests -pl dpc-api
mvn jacoco:report

echo "┌──────────────────────────────────────────┐"
echo "│                                          │"
echo "│             All Tests Complete           │"
echo "│                                          │"
echo "└──────────────────────────────────────────┘"