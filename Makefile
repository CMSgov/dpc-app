IG_PUBLISHER = ./.bin/org.hl7.fhir.publisher.jar 
REPORT_COVERAGE ?= false

${IG_PUBLISHER}:
	-mkdir ./.bin
	curl https://fhir.github.io/latest-ig-publisher/org.hl7.fhir.publisher.jar -o ${IG_PUBLISHER}

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