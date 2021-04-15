.PHONY: unit-test
unit-test:
	@bash ./unit_test.sh

.PHONY: lint
lint:
	@echo "-------------- LINTING STARTED --------------"
	@golangci-lint run && echo "*********** LINT FREE!! ***********" || echo "*********** LINTING FAILED!! ***********"
	@echo "-------------- SECURITY SCAN STARTED --------------"
	@cd src && gosec -fmt=json -out=../results.json ./... && cd ..
	@echo "*********** SECURITY SCAN RESULTS: ***********"
	@cat results.json

.PHONY: test
test:  unit-test lint

