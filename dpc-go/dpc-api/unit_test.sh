#!/bin/bash
#
# Run all API unit tests and coverage
#
set -e
set -o pipefail

timestamp=`date +%Y-%m-%d_%H-%M-%S`
mkdir -p test_results/${timestamp}
mkdir -p test_results/latest

cd src
echo -e "-------------- INSTALL LINTER --------------"
curl -sSfL https://raw.githubusercontent.com/golangci/golangci-lint/master/install.sh | sh -s -- -b $(go env GOPATH)/bin
curl -sSfL https://raw.githubusercontent.com/securego/gosec/master/install.sh | sh -s -- -b $(go env GOPATH)/bin
echo -e "-------------- DPC-API LINTING STARTED --------------"
golangci-lint --timeout 5m run && echo "*********** DPC-API IS LINT FREE!! ***********" || echo -e "*********** LINTING FAILED!! ***********"
echo -e "-------------- SECURITY SCAN STARTED --------------"
gosec -fmt=json -out=../results.json -stdout *.go
echo -e "*********** SECURITY SCAN RESULTS: ***********"
cat ../results.json

echo -e "Running GO DPC-API unit tests and placing them in test_results/${timestamp}..."
gotestsum --debug --junitfile ../test_results/${timestamp}/junit.xml -- -p 1 -race ./... -coverprofile ../test_results/${timestamp}/testcoverage.out 2>&1 | tee ../test_results/${timestamp}/testresults.out | sed ''/PASS/s//$(printf "\033[32mPASS\033[0m")/'' | sed ''/FAIL/s//$(printf "\033[31mFAIL\033[0m")/''
go tool cover -func ../test_results/${timestamp}/testcoverage.out > ../test_results/${timestamp}/testcov_byfunc.out
echo TOTAL COVERAGE:  $(tail -1 ../test_results/${timestamp}/testcov_byfunc.out | head -1)
go tool cover -html=../test_results/${timestamp}/testcoverage.out -o ../test_results/${timestamp}/testcoverage.html
cp ../test_results/${timestamp}/* ../test_results/latest
cd ..
echo -e "DPC-API unit tests have completed."
