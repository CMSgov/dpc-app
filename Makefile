IG_PUBLISHER = ./.bin/org.hl7.fhir.publisher.jar
REPORT_COVERAGE ?= false

SMOKE_THREADS ?= 10

${IG_PUBLISHER}:
	-mkdir ./.bin
	curl https://fhir.github.io/latest-ig-publisher/org.hl7.fhir.publisher.jar -o ${IG_PUBLISHER}

venv: venv/bin/activate

venv/bin/activate: requirements.txt
	test -d venv || virtualenv venv
	. venv/bin/activate; pip install -Ur requirements.txt
	touch venv/bin/activate



.PHONY: ig/publish
ig/publish: ${IG_PUBLISHER}
	@echo "Building Implementation Guide"
	@java -jar ${IG_PUBLISHER} -ig ig/ig.json

.PHONY: travis
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

.PHONY: smoke/dev
smoke/dev: venv smoke
	@echo "Running Smoke Tests against Development env"
	. venv/bin/activate; bzt src/test/dev.smoke_test.yml

.PHONY: smoke/test
smoke/test: venv smoke
	. venv/bin/activate; bzt src/test/test.smoke_test.yml

.PHONY: smoke/prod-sbx
smoke/prod-sbx: venv smoke
	@echo "Running Smoke Tests against Sandbox env"
	. venv/bin/activate; bzt src/test/prod-sbx.smoke_test.yml

.PHONY: docker-base
docker-base:
	@docker-compose -f ./docker-compose.base.yml build base

.PHONY: secure-envs
secure-envs:
	@export $(bash ops/scripts/secrets --decrypt | tail -n +3 | sed -e'/^$/d' -e '/^#/d' | xargs)
