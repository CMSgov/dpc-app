.PHONY: unit-test
unit-test:
	@bash ./unit_test.sh

.PHONY: test
test:
	$(MAKE) unit-test

.PHONY: test-docker
test-docker:
	@docker-compose -f ./docker-compose.test.yml up --build run-test -exit-code-from dpc-api_run-test_1

