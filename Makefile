REPORT_COVERAGE ?= false

SMOKE_THREADS ?= 10

venv: venv/bin/activate

venv/bin/activate: requirements.txt
	test -d venv || python3 -m venv venv
	. venv/bin/activate
	touch venv/bin/activate

.PHONY: website
website:
	@docker build -f dpc-web/Dockerfile . -t dpc-web

.PHONY: admin
admin:
	@docker build -f dpc-admin/Dockerfile . -t dpc-web-admin

.PHONY: impl
impl:
	@docker build -f dpc-impl/Dockerfile . -t dpc-impl

.PHONY: adminv2
adminv2:
	@docker build -f dpc-adminv2/Dockerfile . -t dpc-adminv2

.PHONY: start-app
start-app: secure-envs
	@docker-compose up start_core_dependencies
	@USE_BFD_MOCK=false docker-compose up start_api_dependencies
	@docker-compose up start_api

.PHONY: start-local
start-local: secure-envs
	@docker-compose -f docker-compose.yml -f docker-compose-local.yml up start_api_dependencies

.PHONY: start-local-api
start-local-api: secure-envs start-local
	@docker-compose -f docker-compose.yml -f docker-compose-local.yml up start_api

.PHONY: start-portals
start-portals:
	@docker-compose -p dpc-v2 -f docker-compose.yml -f docker-compose.portals.yml up start_core_dependencies
	@docker-compose -p dpc-v2 -f docker-compose.yml -f docker-compose.portals.yml up start_web
	@docker-compose -p dpc-v2 -f docker-compose.yml -f docker-compose.portals.yml up start_admin
	@docker-compose -p dpc-v2 -f docker-compose.yml -f docker-compose.portals.yml up start_impl
	@docker-compose -p dpc-v2 -f docker-compose.yml -f docker-compose.portals.yml up start_adminv2
	@docker ps

.PHONY: down-portals
down-portals:
	@docker-compose -p dpc-v2 -f docker-compose.yml -f docker-compose.portals.yml down

.PHONY: down-start-v2-portals
down-start-v2-portals:
	@docker-compose -p start-v2-portals -f docker-compose.yml -f docker-compose.portals.yml down

.PHONY: down-start-v1-portals
down-start-v1-portals:
	@docker-compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml down

.PHONY: start-dpc
start-dpc: secure-envs
	@docker-compose -f docker-compose.yml -f docker-compose.portals.yml up start_core_dependencies
	@USE_BFD_MOCK=false docker-compose -f docker-compose.yml -f docker-compose.portals.yml up start_api_dependencies
	@docker-compose -f docker-compose.yml -f docker-compose.portals.yml up start_api
	@docker-compose -f docker-compose.yml -f docker-compose.portals.yml up start_web
	@docker-compose -f docker-compose.yml -f docker-compose.portals.yml up start_admin
	@docker-compose -f docker-compose.yml -f docker-compose.portals.yml up start_impl
	@docker ps

.PHONY: down-dpc
down-dpc: 
	@docker-compose -f docker-compose.yml -f docker-compose.portals.yml down
	@docker ps

.PHONY: seed-db
seed-db:
	@java -jar dpc-attribution/target/dpc-attribution.jar db migrate && java -jar dpc-attribution/target/dpc-attribution.jar seed

.PHONY: ci-app
ci-app: docker-base secure-envs
	@./dpc-test.sh

.PHONY: ci-portals
ci-portals: secure-envs
	@./dpc-portals-test.sh

.PHONY: ci-portals-v1
ci-portals-v1: secure-envs
	@./dpcv1-portals-test.sh

.PHONY: ci-admin-portal
ci-admin-portal: secure-envs
	@./dpc-admin-portal-test.sh

.PHONY: ci-web-portal
ci-web-portal: secure-envs
	@./dpc-web-portal-test.sh

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

.PHONY: docker-base
docker-base:
	@docker-compose -f ./docker-compose.base.yml build base

.PHONY: secure-envs
secure-envs:
	@bash ops/scripts/secrets --decrypt ops/config/encrypted/bb.keystore | tail -n +2 > bbcerts/bb.keystore
	@bash ops/scripts/secrets --decrypt ops/config/encrypted/local.env | tail -n +2 > ops/config/decrypted/local.env

.PHONY: maven-config
maven-config:
	@mkdir -p ./.mvn
	@: > ./.mvn/maven.config
	@while read line;do echo "-D$${line} " >> ./.mvn/maven.config;done < ./ops/config/decrypted/local.env

.PHONY: unit-tests
unit-tests:
	@bash ./dpc-unit-test.sh
