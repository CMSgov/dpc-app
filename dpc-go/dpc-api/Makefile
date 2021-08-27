.PHONY: unit-test
unit-test:
	@bash ./unit_test.sh

.PHONY: test
test:
	$(MAKE) unit-test

.PHONY: test-docker
test-docker:
	@docker-compose -f ./docker-compose.test.yml up --build run-test 
	@docker-compose -f ./docker-compost.test.yml up --exit-code-from run-test
