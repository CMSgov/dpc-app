IG_PUBLISHER = ./.bin/org.hl7.fhir.publisher.jar 
REPORT_COVERAGE ?= false

${IG_PUBLISHER}:
	-mkdir ./.bin
	curl https://fhir.github.io/latest-ig-publisher/org.hl7.fhir.publisher.jar -o ${IG_PUBLISHER}

.PHONY: ig/publish
ig/publish: ${IG_PUBLISHER}
	@echo "making IG"
	@java -jar ${IG_PUBLISHER} -ig ig/ig.json

travis:
	@./dpc-test.sh
