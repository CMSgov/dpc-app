REPORT_COVERAGE ?= false

# Smoke Testing
# ==============

SMOKE_THREADS ?= 10

venv: venv/bin/activate

venv/bin/activate: requirements.txt
	test -d venv || python3 -m venv venv
	. venv/bin/activate
	touch venv/bin/activate

.PHONY: smoke
smoke:
	@mvn clean package -DskipTests -Djib.skip=True -pl dpc-smoketest -am -ntp

.PHONY: smoke/local
smoke/local: venv smoke
	@echo "Running Smoke Tests against Local env"
	@read -p "`echo '\n=====\nThe Smoke Tests require an authenticated environment!\nVerify your local API environment has \"authenticationDisabled = false\" or these tests will fail.\n=====\n\nPress ENTER to run the tests...'`"
	. venv/bin/activate; pip install -Ur requirements.txt; bzt src/test/local.smoke_test.yml

.PHONY: smoke/remote
smoke/remote: venv smoke
	@echo "Running Smoke Tests against ${HOST_URL}"
	. venv/bin/activate; bzt src/test/remote.smoke_test.yml

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
	@docker-compose up start_core_dependencies

start-api-dependencies: # Start internal Java service dependencies, e.g. attribution and aggregation services.
start-api-dependencies:
	@USE_BFD_MOCK=false docker-compose up start_api_dependencies

start-app: ## Start the API
start-app: secure-envs start-db start-api-dependencies
	@docker-compose up start_api

start-api: ## Start the API
start-api: start-app

start-web: ## Start the sandbox portal
start-web:
	@docker-compose -f docker-compose.yml -f docker-compose.portals.yml up start_web

start-admin: ## Start the sandbox admin portal
start-admin:
	@docker-compose -f docker-compose.yml -f docker-compose.portals.yml up start_admin

start-portal: ## Start the DPC portal
start-portal:
	@docker-compose -f docker-compose.yml -f docker-compose.portals.yml up start_portal

start-portals: ## Start all frontend services
start-portals: start-db start-web start-admin start-portal


# Debug commands
# ==============

.PHONY: start-dpc-debug
start-dpc-debug: secure-envs
	@mvn clean install -Pdebug -DskipTests -ntp
	@docker-compose -f docker-compose.yml -f docker-compose.portals.yml up start_core_dependencies
	@DEBUG_MODE=true USE_BFD_MOCK=false docker-compose -f docker-compose.yml -f docker-compose.portals.yml up start_api_dependencies
	@DEBUG_MODE=true docker-compose -f docker-compose.yml -f docker-compose.portals.yml up start_api
	@docker-compose -f docker-compose.yml -f docker-compose.portals.yml up start_web
	@docker-compose -f docker-compose.yml -f docker-compose.portals.yml up start_admin
	@docker-compose -f docker-compose.yml -f docker-compose.portals.yml up start_portal
	@docker ps

.PHONY: start-app-debug
start-app-debug: secure-envs
	@mvn clean install -Pdebug -DskipTests -ntp
	@docker-compose -f docker-compose.yml -f docker-compose.portals.yml up start_core_dependencies
	@DEBUG_MODE=true USE_BFD_MOCK=false docker-compose -f docker-compose.yml -f docker-compose.portals.yml up start_api_dependencies
	@DEBUG_MODE=true docker-compose -f docker-compose.yml -f docker-compose.portals.yml up start_api

.PHONY: start-it-debug
start-it-debug: secure-envs
	@docker-compose down
	@mvn clean compile -Pdebug -B -V -ntp -DskipTests
	@mvn package -Pci -ntp -DskipTests
	@docker-compose up start_core_dependencies
	@DEBUG_MODE=true docker-compose up start_api_dependencies


# Down commands
# ==============

down-dpc: ## Shut down all services
down-dpc: 
	@docker-compose -f docker-compose.yml -f docker-compose.portals.yml down

down-portals: ## Shut down all services
down-portals: down-dpc


# Utility commands
# =================

secure-envs: ## Decrypt API environment secrets
secure-envs:
	@bash ops/scripts/secrets --decrypt ops/config/encrypted/bb.keystore | tail -n +2 > bbcerts/bb.keystore
	@bash ops/scripts/secrets --decrypt ops/config/encrypted/local.env | tail -n +2 > ops/config/decrypted/local.env

seed-db: ## Seed attribution data for local database
seed-db:
	@java -jar dpc-attribution/target/dpc-attribution.jar db migrate && java -jar dpc-attribution/target/dpc-attribution.jar seed

maven-config: ## Translate local environment variables into maven.config for manual API installation
maven-config:
	@mkdir -p ./.mvn
	@: > ./.mvn/maven.config
	@while read line;do echo "-D$${line} " >> ./.mvn/maven.config;done < ./ops/config/decrypted/local.env

psql: ## Run a psql shell
	@docker-compose -f docker-compose.yml exec -it db psql -U postgres

portal-console: ## Run a rails console shell
	@docker-compose -f docker-compose.yml -f docker-compose.portals.yml exec -it dpc_portal rails console


# Build & Test commands
# ======================

.PHONY: docker-base
docker-base:
	@docker-compose -f ./docker-compose.base.yml build base

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

.PHONY: ci-web-portal
ci-web-portal: secure-envs
	@./dpc-web-portal-test.sh

.PHONY: ci-api-client
ci-api-client:
	@./dpc-api-client-test.sh

.PHONY: unit-tests
unit-tests:
	@bash ./dpc-unit-test.sh
