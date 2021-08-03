.PHONY: unit-test
unit-test:
	@bash ./unit_test.sh

.PHONY: lint
lint:
	@echo "-------------- ATTRIBUTION LINTING STARTED --------------"
	@cd src && golangci-lint run && echo "*********** ATTRIBUTION IS LINT FREE!! ***********" || echo "*********** LINTING FAILED!! ***********"
	@echo "-------------- SECURITY SCAN STARTED --------------"
	@cd src && gosec -fmt=json -out=../results.json ./...
	@echo "*********** SECURITY SCAN RESULTS: ***********"
	@cat results.json

.PHONY: test
test:  lint unit-test

.PHONY: bfd-certs
bfd-certs:
	@bash ops/scripts/secrets --decrypt shared_files/encrypted/bfd-dev-test-ca-file.crt | tail -n +2 > shared_files/decrypted/bfd-dev-test-ca-file.crt
	@bash ops/scripts/secrets --decrypt shared_files/encrypted/bfd-dev-test-cert.pem | tail -n +2 > shared_files/decrypted/bfd-dev-test-cert.pem
	@bash ops/scripts/secrets --decrypt shared_files/encrypted/bfd-dev-test-key.pem | tail -n +2 > shared_files/decrypted/bfd-dev-test-key.pem