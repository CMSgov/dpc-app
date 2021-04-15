.PHONY: unit-test
unit-test:
	@bash ./unit_test.sh

.PHONY: lint
lint:
	@echo "-------------- ATTRIBUTION LINTING STARTED --------------"
	@golangci-lint run && echo "*********** ATTRIBUTION IS LINT FREE!! ***********" || echo "*********** LINTING FAILED!! ***********"
	@echo "-------------- SECURITY SCAN STARTED --------------"
	@cd src && gosec -fmt=json -out=../results.json ./...
	@echo "*********** SECURITY SCAN RESULTS: ***********"
	@cat results.json

.PHONY: test
test:  unit-test lint
