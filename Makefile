IG_PUBLISHER = ./.bin/org.hl7.fhir.publisher.jar
REPORT_COVERAGE ?= false

SMOKE_THREADS ?= 10

${IG_PUBLISHER}:
	-mkdir ./.bin
	curl https://storage.googleapis.com/ig-build/org.hl7.fhir.publisher.jar -o ${IG_PUBLISHER}

venv: venv/bin/activate

venv/bin/activate: requirements.txt
	test -d venv || virtualenv venv
	. venv/bin/activate; CRYPTOGRAPHY_DONT_BUILD_RUST=1 pip install -Ur requirements.txt
	touch venv/bin/activate



.PHONY: ig/publish
ig/publish: ${IG_PUBLISHER}
	@echo "Building Implementation Guide"
	@java -jar ${IG_PUBLISHER} -ig ig/ig.json

.PHONY: website
website:
	@docker build -f dpc-web/Dockerfile . -t dpc-web

.PHONY: admin
admin:
	@docker build -f dpc-admin/Dockerfile . -t dpc-web-admin

.PHONY: impl
impl:
	@docker build -f dpc-impl/Dockerfile . -t dpc-impl

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
	# @docker-compose -p dpc-v2 -f docker-compose.yml -f docker-compose.portals.yml up start_web
	# @docker-compose -p dpc-v2 -f docker-compose.yml -f docker-compose.portals.yml up start_admin
	@docker-compose -p dpc-v2 -f docker-compose.yml -f docker-compose.portals.yml up start_impl
	@docker ps

.PHONY: down-portals
down-portals:
	@docker-compose -p dpc-v2 -f docker-compose.yml -f docker-compose.portals.yml down

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

.PHONY: start-v2
start-v2: secure-envs
	@docker-compose -p dpc-v2 -f docker-compose.yml -f docker-compose.v2.yml up start_core_dependencies
	@docker-compose -p dpc-v2 -f docker-compose.yml -f docker-compose.v2.yml up start_api_dependencies
	@docker-compose -p dpc-v2 -f docker-compose.yml -f docker-compose.v2.yml -f dpc-go/dpc-attribution/docker-compose.yml up -d attribution2
	@docker-compose -p dpc-v2 -f dpc-go/dpc-api/docker-compose.yml up -d api
	@docker ps

.PHONY: down-v2
down-v2:
	@docker-compose -p dpc-v2 -f docker-compose.yml -f dpc-go/dpc-attribution/docker-compose.yml down
	@docker-compose -p dpc-v2 -f dpc-go/dpc-api/docker-compose.yml down
	@docker-compose -p dpc-v2 -f docker-compose.yml -f docker-compose.v2.yml down
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

.PHONY: smoke
smoke:
	@mvn clean package -DskipTests -Djib.skip=True -pl dpc-smoketest -am -ntp

.PHONY: smoke/local
smoke/local: venv smoke
	@echo "Running Smoke Tests against Local env"
	@read -p "`echo '\n=====\nThe Smoke Tests require an authenticated environment!\nVerify your local API environment has \"authenticationDisabled = false\" or these tests will fail.\n=====\n\nPress ENTER to run the tests...'`"
	. venv/bin/activate; bzt src/test/local.smoke_test.yml

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

.PHONE: unit-tests
unit-tests:
	@bash ./dpc-unit-test.sh