IG_PUBLISHER = ./.bin/org.hl7.fhir.publisher.jar
REPORT_COVERAGE ?= false

SMOKE_THREADS ?= 10

${IG_PUBLISHER}:
	-mkdir ./.bin
	curl https://storage.googleapis.com/ig-build/org.hl7.fhir.publisher.jar -o ${IG_PUBLISHER}

venv: venv/bin/activate

venv/bin/activate: requirements.txt
	test -d venv || virtualenv venv
	. venv/bin/activate; pip install -Ur requirements.txt
	touch venv/bin/activate



.PHONY: ig/publish
ig/publish: ${IG_PUBLISHER}
	@echo "Building Implementation Guide"
	@java -jar ${IG_PUBLISHER} -ig ig/ig.json

.PHONY: travis secure-envs
travis:
	@./dpc-test.sh

.PHONY: website
website:
	@docker build -f dpc-web/Dockerfile . -t dpc-web

.PHONY: start-app
start-app: secure-envs
	@docker-compose up start_core_dependencies
	@docker-compose up start_api_dependencies
	@docker-compose up start_api

.PHONY: ci-app
ci-app: docker-base secure-envs
	@./dpc-test.sh

.PHONY: ci-web
ci-web:
	@./dpc-web-test.sh

.PHONY: smoke
smoke:
	@mvn clean package -DskipTests -Djib.skip=True -pl dpc-smoketest -am

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

