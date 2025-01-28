# Smoke Testing
# ==============

SMOKE_THREADS ?= 10

venv: venv/bin/activate

venv/bin/activate: requirements.txt
	test -d venv || python3 -m venv venv
	. venv/bin/activate
	touch venv/bin/activate

.PHONY: smoke
smoke: ## If running on Jenkins, purges JMeter's library, then copies all of dpc-smoketests dependencies into it
smoke:
	@mvn clean package -DskipTests -Djib.skip=True -pl dpc-smoketest -am -ntp

.PHONY: smoke/local
smoke/local: export USE_BFD_MOCK=false
smoke/local: export AUTH_DISABLED=false
smoke/local: venv smoke start-dpc
	@echo "Running Smoke Tests against Local env"
	. venv/bin/activate; pip install -Ur requirements.txt; bzt src/test/local.smoke_test.yml

.PHONY: smoke/remote
smoke/remote: smoke
	@echo "Running Smoke Tests against ${HOST_URL}"
	. venv/bin/activate; bzt src/test/remote.smoke_test.yml

.PHONY: smoke/sandbox
smoke/sandbox: venv smoke
	@echo "Running Smoke Tests against ${HOST_URL}"
	. venv/bin/activate; bzt src/test/sandbox.smoke_test.yml

.PHONY: smoke/prod
smoke/prod: venv smoke
	@echo "Running Smoke Tests against ${HOST_URL}"
	. venv/bin/activate; bzt src/test/prod.smoke_test.yml


# Build commands
#
# These commands build/compile our applications and docker images.
# To start the applications, use the start-* commands below.
# ==============

api: ## Builds the Java API services
api: secure-envs
	mvn clean compile -Perror-prone -B -V -ntp -T 4 -DskipTests
	mvn package -Pci -ntp -T 4 -DskipTests

website: ## Builds the sandbox portal website
website:
	@docker build -f dpc-web/Dockerfile . -t dpc-web

admin: ## Builds the sandbox admin website
admin:
	@docker build -f dpc-admin/Dockerfile . -t dpc-web-admin

portal: ## Builds the DPC portal
portal:
	mkdir -p dpc-portal/vendor/api_client
	cp -r engines/api_client/ dpc-portal/vendor/api_client/
	@docker build -f dpc-portal/Dockerfile . -t dpc-web-portal


# Start commands
# ==============

start-dpc: ## Start all DPC API and portal services
start-dpc: start-app start-portals

start-db: ## Start the database
start-db:
	@docker compose up db --wait

start-api-dependencies: # Start internal Java service dependencies, e.g. attribution and aggregation services.
start-api-dependencies:
	@docker compose up attribution aggregation --wait

start-app: ## Start the API
start-app: secure-envs start-db start-api-dependencies
	@docker compose up api --wait

start-api: ## Start the API
start-api: start-app

start-web: ## Start the sandbox portal
start-web:
	@docker compose -f docker-compose.yml -f docker-compose.portals.yml up dpc_web --wait

start-admin: ## Start the sandbox admin portal
start-admin:
	@docker compose -f docker-compose.yml -f docker-compose.portals.yml up dpc_admin --wait

start-portal: ## Start the DPC portal
start-portal: secure-envs
	@docker compose -f docker-compose.yml -f docker-compose.portals.yml up dpc_portal --wait

start-portals: ## Start all frontend services
start-portals: start-db start-web start-admin start-portal

start-load-tests: ## Run DPC performance tests locally in a Docker image provided by Grafana/K6
start-load-tests: secure-envs
	@docker run --rm -v $(shell pwd)/dpc-load-testing:/src --env-file $(shell pwd)/ops/config/decrypted/local.env -e ENVIRONMENT=local -i grafana/k6 run /src/script.js


# Debug commands
# ==============

.PHONY: start-dpc-debug
start-dpc-debug: secure-envs
	@mvn clean install -Pci -Pdebug -DskipTests -ntp
	@DEBUG_MODE=true docker compose -f docker-compose.yml up aggregation api --wait
	@docker compose -f docker-compose.yml -f docker-compose.portals.yml up dpc_web dpc_admin dpc_portal --wait
	@docker ps

.PHONY: start-app-debug
start-app-debug: secure-envs
	@docker compose down
	@mvn clean install -Pci -Pdebug -DskipTests -ntp
	@DEBUG_MODE=true docker compose -f docker-compose.yml up api aggregation --wait

.PHONY: start-it-debug
start-it-debug: secure-envs
	@docker compose down
	@mvn clean install -Pci -Pdebug -DskipTests -ntp
	@DEBUG_MODE=true docker compose up attribution aggregation --wait


# Down commands
# ==============

down-dpc: ## Shut down all services
down-dpc:
	@docker compose -f docker-compose.yml -f docker-compose.portals.yml down

down-portals: ## Shut down all services
down-portals: down-dpc

down-start-v1-portals: ## Shut down test services
down-start-v1-portals:
	@docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml down

# Utility commands
# =================

CONF_FILE = "dpc-attribution/src/test/resources/test.application.yml"

secure-envs: ## Decrypt API environment secrets
secure-envs:
	@bash ops/scripts/secrets --decrypt ops/config/encrypted/bb.keystore | tail -n +2 > bbcerts/bb.keystore
	@bash ops/scripts/secrets --decrypt ops/config/encrypted/local.env | tail -n +2 > ops/config/decrypted/local.env

seed-db: ## Seed attribution data for local database
seed-db:
	@java -jar dpc-attribution/target/dpc-attribution.jar db migrate $(CONF_FILE)
	@java -jar dpc-attribution/target/dpc-attribution.jar seed $(CONF_FILE)

maven-config: ## Translate local environment variables into maven.config for manual API installation
maven-config:
	@./maven-config.sh

psql: ## Run a psql shell
	@docker compose -f docker-compose.yml exec -it db psql -U postgres

portal-sh: ## Run a portal shell
	@docker compose -f docker-compose.yml -f docker-compose.portals.yml exec -it dpc_portal bin/sh

portal-console: ## Run a rails console shell
	@docker compose -f docker-compose.yml -f docker-compose.portals.yml exec -it dpc_portal bin/console


# Build & Test commands
# ======================

.PHONY: docker-base
docker-base:
	@docker compose -f ./docker-compose.base.yml build base

.PHONY: ci-app
ci-app: docker-base secure-envs
	@./dpc-test.sh

.PHONY: ci-portals
ci-portals: secure-envs
	@./dpc-portals-test.sh

.PHONY: ci-portals-v1
ci-portals-v1: secure-envs
	@./dpc-portals-test.sh

.PHONY: ci-admin-portal
ci-admin-portal: secure-envs
	@./dpc-admin-portal-test.sh

.PHONY: ci-portal
ci-portal: secure-envs
	@./dpc-portal-test.sh

.PHONY: ci-portal-accessibility
ci-portal-accessibility: secure-envs
	@./dpc-portal-accessibility-test.sh

.PHONY: ci-web-portal
ci-web-portal: secure-envs
	@./dpc-web-portal-test.sh

.PHONY: ci-api-client
ci-api-client:
	@./dpc-api-client-test.sh

.PHONY: unit-tests
unit-tests:
	@bash ./dpc-unit-test.sh
