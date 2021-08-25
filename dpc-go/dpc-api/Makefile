.PHONY: unit-test
unit-test:
	@bash ./unit_test.sh

.PHONY: lint
lint:
	@echo "-------------- DPC-API LINTING STARTED --------------"
	@cd src && golangci-lint run && echo "*********** DPC-API IS LINT FREE!! ***********" || echo "*********** LINTING FAILED!! ***********"
	@echo "-------------- SECURITY SCAN STARTED --------------"
	@cd src && gosec -fmt=json -out=../results.json ./...
	@echo "*********** SECURITY SCAN RESULTS: ***********"
	@cat results.json

.PHONY: test
test:  lint unit-test

.PHONY: smoke
smoke:
	@docker-compose -f docker-compose.test.yml run --rm smoke_test test/smoke/api_smoke_tests.postman_collection.json -e test/smoke/local.postman_environment.json
