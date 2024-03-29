# opt-out-lambda
opt out lambda readme

## How to test s3 locally and get a coverage report
make sure you have aws cli downloaded on your computer
configure aws with fake data, `aws configure`
- run `docker-compose up -d`
- run populate_s3.sh
- run `aws --endpoint-url=http://localhost:4566 s3api list-objects --bucket demo-bucket` to verify dummyfile.txt and contents of `./synthetic_test_data` was added to s3 in localstack
- run `make test`
    - This will print out a coverage percentage and a report at `coverage/unit_coverage.html`.

## How to run locally
- Pull down dpc-app and run `make start-app` or just `docker-compose up start_core_dependencies` to start a local copy of the dpc-consent database.
- run `docker-compose up -d` in this app's directory
- run populate_s3.sh
- run `aws --endpoint-url=http://localhost:4566 s3api list-objects --bucket demo-bucket` to verify files were added to s3 in localstack
- run `make run-local`
- run `aws --endpoint-url=http://localhost:4566 s3api list-objects --bucket demo-bucket` to verify that a response file was created
- run `aws --endpoint-url=http://localhost:4566 s3api get-object --bucket demo-bucket --key T#EFT.ON.ACO.NGD1800.DPRF.D181120.T1000009_response response_file.txt` to download the response file and verify it's correct

## How to test the build locally and get a coverage report
- Pull down dpc-app and run `make start-app` or just `docker-compose up start_core_dependencies` to start a local copy of the dpc-consent database.
- run `docker-compose up -d` in this app's directory
- run populate_s3.sh
- run `make test-build`
    - This will build the app with instrumentation, run it, print out a coverage percentage and a report at `coverage/int_coverage.html`

## A Note on Integration Testing
- If you're writing a tests that asserts a particular interaction with some other service, this is an integration test. These functions should be prefixed with `TestIntegration`.
- Run integration tests with `make integration-test`.
