.PHONY: unit-test
unit-test:
	@bash ./unit_test.sh

.PHONY: test
test:
	$(MAKE) unit-test


.PHONY: smoke
smoke:
	@docker-compose -f docker-compose.test.yml run --rm smoke_test test/smoke/api_smoke_tests.postman_collection.json -e test/smoke/local.postman_environment.json

.PHONY: test-docker
test-docker:
	@docker-compose -f ./docker-compose.test.yml up --build --exit-code-from run-test
