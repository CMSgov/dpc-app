.PHONY: unit-test
unit-test:
	@bash ./unit_test.sh

.PHONY: lint
lint:
	@echo "-------------- DPC-API LINTING STARTED --------------"
	@cd src && golangci-lint --timeout 5m run && echo "*********** DPC-API IS LINT FREE!! ***********" || echo "*********** LINTING FAILED!! ***********"
	@echo "-------------- SECURITY SCAN STARTED --------------"
	@cd src && gosec -fmt=json -out=../results.json ./...
	@echo "*********** SECURITY SCAN RESULTS: ***********"
	@cat results.json

.PHONY: test
test:
	$(MAKE) lint
	$(MAKE) unit-test

.PHONY: test-docker
test-docker:
	@docker-compose -f ./docker-compose.test.yml up --build run-test

