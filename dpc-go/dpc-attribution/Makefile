.PHONY: unit-test
unit-test:
	@bash ./unit_test.sh

.PHONY: test
test:
	$(MAKE) unit-test

.PHONY: bfd-certs
bfd-certs:
	@bash ops/scripts/secrets --decrypt shared_files/encrypted/local.env | tail -n +2 > shared_files/decrypted/local.env
	@bash ops/scripts/secrets --decrypt shared_files/encrypted/bfd-dev-test-ca-file.crt | tail -n +2 > shared_files/decrypted/bfd-dev-test-ca-file.crt
	@bash ops/scripts/secrets --decrypt shared_files/encrypted/bfd-dev-test-cert.pem | tail -n +2 > shared_files/decrypted/bfd-dev-test-cert.pem
	@bash ops/scripts/secrets --decrypt shared_files/encrypted/bfd-dev-test-key.pem | tail -n +2 > shared_files/decrypted/bfd-dev-test-key.pem

.PHONY: test-docker
test-docker: bfd-certs
	@docker-compose -f ./docker-compose.test.yml up --build run-test
	@docker-compose -f ./docker-compose.test.yml up --exit-code-from run-test