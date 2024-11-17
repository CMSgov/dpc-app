IS_AWS_EC2="-fdocker-compose.override.yml"
REPORT_COVERAGE ?= false

ifdef DOCKER_PROJECT_NAME
        DOCKER_PROJ:="-p${DOCKER_PROJECT_NAME}"
else
        DOCKER_PROJ:=
endif

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
smoke: ## If running on Jenkins, purges JMeter's library, then copies all of dpc-smoketests dependencies into it
smoke:
	@JENKINS_DIR="/var/jenkins_home/.bzt/jmeter-taurus/5.5/lib"; \
	if [ -d $$JENKINS_DIR ]; then \
		echo "Rebuilding JMeter lib"; \
		rm $$JENKINS_DIR/*.jar; \
		mvn clean install -DskipTests -Djib.skip=True -pl dpc-common -am -ntp; \
		mvn dependency:copy-dependencies -pl dpc-smoketest -DoutputDirectory=$$JENKINS_DIR; \
	else \
		echo "Not running on Jenkins"; \
	fi
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

.PHONY: smoke/sandbox
smoke/sandbox: venv smoke
	@echo "Running Smoke Tests against ${HOST_URL}"
	. venv/bin/activate; bzt src/test/sandbox.smoke_test.yml

.PHONY: smoke/prod
smoke/prod: venv smoke
	@echo "Running Smoke Tests against ${HOST_URL}"
	. venv/bin/activate; bzt src/test/prod.smoke_test.yml


# Build targets
#
# These targets build/compile our docker images.
# ==============

.PHONY: docker-base
docker-base:
	@docker compose -f ./docker-compose.base.yml build base

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


# Start targets
# These targets bring up the docker containers hosting DPC system components
# ==============

start-dpc: ## Start all DPC API and portal services
start-dpc: start-app start-portals

start-db: ## Start the postgres database supporting the api
start-db:
	@docker compose $(DOCKER_PROJ) -f docker-compose.yml $(IS_AWS_EC2) up --wait -d db

start-redis: ## Start the redis database supporting the portal
start-redis:
	@docker compose $(DOCKER_PROJ) -f docker-compose.yml -f docker-compose.portals.yml up --wait -d redis

start-portal-dbs: ## Start the postgres and redis database supporting the portal
start-portal-dbs: start-db start-redis

start-consent: ## Start the consent service supporting the api
start-consent:
	$(eval DEBUG_ARG := $(if $(filter true,$(DEBUG_MODE)),-f docker-compose.debug-override.yml,))
	@docker compose $(DOCKER_PROJ) -f docker-compose.yml $(IS_AWS_EC2) $(DEBUG_ARG) up --wait -d consent

start-attribution: ## Start the attribution service supporting the api
start-attribution:
	$(eval DEBUG_ARG := $(if $(filter true,$(DEBUG_MODE)),-f docker-compose.debug-override.yml,))
	@docker compose $(DOCKER_PROJ) -f docker-compose.yml $(IS_AWS_EC2) $(DEBUG_ARG) up --wait -d attribution

start-aggregation: ## Start the aggregation service supporting the api
start-aggregation:
	$(eval DEBUG_ARG := $(if $(filter true,$(DEBUG_MODE)),-f docker-compose.debug-override.yml,))
	@docker compose $(DOCKER_PROJ) -f docker-compose.yml $(IS_AWS_EC2) $(DEBUG_ARG) up --wait -d aggregation

start-api-dependencies: # Start internal Java service dependencies, e.g. attribution and aggregation services.
start-api-dependencies: start-attribution 
	@USE_BFD_MOCK=false make start-aggregation

start-mock-api-dependencies: # Start internal Java service dependencies, e.g. attribution and aggregation services with mock BFD.
start-mock-api-dependencies: start-attribution start-aggregation

start-mock-app: ## Start the API with mock BFD
start-mock-app: secure-envs
	@docker compose $(DOCKER_PROJ) -f docker-compose.yml $(IS_AWS_EC2) up --wait -d api

start-app: ## Start the API
start-app: secure-envs 
	@USE_BFD_MOCK=false docker compose $(DOCKER_PROJ) -f docker-compose.yml $(IS_AWS_EC2) up --wait -d api

start-api: ## Start the API
start-api: start-app

start-web: ## Start the sandbox portal
start-web: 
	@docker compose $(DOCKER_PROJ) -f docker-compose.yml $(IS_AWS_EC2) -f docker-compose.portals.yml up --wait -d dpc_web

start-admin: ## Start the sandbox admin portal
start-admin:
	@docker compose $(DOCKER_PROJ) -f docker-compose.yml $(IS_AWS_EC2) -f docker-compose.portals.yml up --wait -d dpc_admin

start-portal: ## Start the DPC portal
start-portal: secure-envs
	@docker compose $(DOCKER_PROJ) -f docker-compose.yml $(IS_AWS_EC2) -f docker-compose.portals.yml up --wait -d dpc_portal

start-portals: ## Start all frontend services
start-portals: start-web start-admin start-portal

start-portals-no-auth: ## Start the web sites with authentication disabled
start-portals-no-auth: 
	@AUTH_DISABLED=true make start-portals

start-system-smoke: ## Start the system for local smoke tests
start-system-smoke: start-api start-portals-no-auth


# Debug commands
# These targets compile the DPC software with debugging info 
# and open debug ports for attaching a remote debugger
# See docker-compose.debug-override.yml for debug port info
# ==============

.PHONY: start-dpc-debug
start-dpc-debug: secure-envs
	@mvn clean install -Pdebug -DskipTests -ntp
	@make start-db start-redis
	@DEBUG_MODE=true make start-api
	@docker make start-portals

.PHONY: start-app-debug
start-app-debug: secure-envs
	@docker compose $(DOCKER_PROJ) down
	@mvn clean compile -Pdebug -DskipTests -ntp
	@mvn package -Pci -ntp -DskipTests
	@make start-db start-redis
	@DEBUG_MODE=true make start-api

.PHONY: start-it-debug
start-it-debug: secure-envs
	@docker compose $(DOCKER_PROJ) down
	@mvn clean compile -Pdebug -B -V -ntp -DskipTests
	@mvn package -Pci -ntp -DskipTests
	@make start-db
	@DEBUG_MODE=true make start-api-dependencies


# Down targets
# These targets bring down dpc docker containers
# ==============

down-dpc: ## Shut down all services
down-dpc:
	@docker compose $(DOCKER_PROJ) -f docker-compose.yml $(IS_AWS_EC2) -f docker-compose.portals.yml down

# Utility targets
# These targets provider CLI support
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
	@mkdir -p ./.mvn
	@: > ./.mvn/maven.config
	@while read line;do echo "-D$${line} " >> ./.mvn/maven.config;done < ./ops/config/decrypted/local.env

psql: ## Run a psql shell
	@docker compose $(DOCKER_PROJ) -f docker-compose.yml $(IS_AWS_EC2) exec -it db psql -U postgres

portal-sh: ## Run a portal shell
	@docker compose $(DOCKER_PROJ) -f docker-compose.yml $(IS_AWS_EC2) -f docker-compose.portals.yml exec -it dpc_portal bin/sh

portal-console: ## Run a rails console shell
	@docker compose $(DOCKER_PROJ) -f docker-compose.yml $(IS_AWS_EC2) -f docker-compose.portals.yml exec -it dpc_portal bin/console


# Software Build & Test targets
# These targets build the software and run tests
# ======================

api: ## Builds the Java API services
api: secure-envs
	mvn clean compile -Perror-prone -B -V -ntp -T 4 -DskipTests
	mvn package -Pci -ntp -T 4 -DskipTests

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

.PHONY: int-tests
int-tests: 
	@docker compose $(DOCKER_PROJ) -f docker-compose.yml $(IS_AWS_EC2) up tests
	@docker compose $(DOCKER_PROJ) -f docker-compose.yml $(IS_AWS_EC2) down

.PHONY: int-tests-cicd
int-tests-cicd: 
	@TEST_VERBOSITY=true docker compose $(DOCKER_PROJ) -f docker-compose.yml $(IS_AWS_EC2) up --exit-code-from tests tests
	@docker compose $(DOCKER_PROJ) -f docker-compose.yml $(IS_AWS_EC2) down

.PHONY: sys-tests
sys-tests:
	@AUTH_DISABLED=true make start-mock-app
	@npm run test
	@docker compose $(PROJECT_NAME) down -t 60
	@if [ -n "$REPORT_COVERAGE" ]; then mvn jacoco:report-integration -Pci -ntp; fi
