IG_PUBLISHER = ./.bin/org.hl7.fhir.publisher.jar
REPORT_COVERAGE ?= false

JMETER = ./.bin/jmeter/bin/jmeter
SMOKE_THREADS ?= 10

${IG_PUBLISHER}:
	-mkdir ./.bin
	curl https://fhir.github.io/latest-ig-publisher/org.hl7.fhir.publisher.jar -o ${IG_PUBLISHER}

./.bin/jmeter.tgz:
	-mkdir ./.bin
	curl http://mirrors.ibiblio.org/apache/jmeter/binaries/apache-jmeter-5.1.1.tgz -o ./.bin/jmeter.tgz

${JMETER}: ./.bin/jmeter.tgz
	-mkdir ./.bin/jmeter
	tar -xvf ./.bin/jmeter.tgz -C ./.bin/jmeter --strip-components 1

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

.PHONY: ci-app
ci-app:
	@./dpc-test.sh

.PHONY: ci-web
ci-web:
	@./dpc-web-test.sh

.PHONY: smoke/test
smoke/test: ${JMETER}
	@echo "Running Smoke Tests against Test env"
	@${JMETER} -p src/main/resources/test.properties -Jthreads=${SMOKE_THREADS} -n -t src/main/resources/SmokeTest.jmx -l out.jtl

.PHONY: smoke/prod-sbx
smoke/prod-sbx: ${JMETER}
	@echo "Running Smoke Tests against Sandbox env"
	@${JMETER} -p src/main/resources/prod-sbx.properties -Jthreads=${SMOKE_THREADS} -n -t src/main/resources/SmokeTest.jmx -l out.jtl
