#!/bin/bash
#
# Run all ATTRIBUTION unit tests and coverage
#
set -e
set -o pipefail

timestamp=`date +%Y-%m-%d_%H-%M-%S`
mkdir -p test_results/${timestamp}
mkdir -p test_results/latest

cd src
echo "-------------- DPC-API LINTING STARTED --------------"
golangci-lint --timeout 5m run \
 && echo "*********** DPC-API IS LINT FREE!! ***********" \
 || echo "*********** LINTING FAILED!! ***********" && exit 1
echo "-------------- SECURITY SCAN STARTED --------------"
gosec -fmt=json -out=../results.json ./...
echo "*********** SECURITY SCAN RESULTS: ***********"
cat results.json

echo "Running GO DPC-ATTRIBUTION unit tests and placing them in test_results/${timestamp}..."
gotestsum --debug --junitfile ../test_results/${timestamp}/junit.xml -- -p 1 -race ./... -coverprofile ../test_results/${timestamp}/testcoverage.out 2>&1 | tee ../test_results/${timestamp}/testresults.out | sed ''/PASS/s//$(printf "\033[32mPASS\033[0m")/'' | sed ''/FAIL/s//$(printf "\033[31mFAIL\033[0m")/''
go tool cover -func ../test_results/${timestamp}/testcoverage.out > ../test_results/${timestamp}/testcov_byfunc.out
echo TOTAL COVERAGE:  $(tail -1 ../test_results/${timestamp}/testcov_byfunc.out | head -1)
go tool cover -html=../test_results/${timestamp}/testcoverage.out -o ../test_results/${timestamp}/testcoverage.html
cp ../test_results/${timestamp}/* ../test_results/latest
echo "DPC-ATTRIBUTION unit tests have completed."