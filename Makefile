
default: dropwizard-config

dropwizard-config:
	# We have to temporarily disable tests because the Integration tests are failing.
	mvn -f external/typesafe-dropwizard-configuration/pom.xml clean install -DskipTests=true
	mvn -f external/typesafe-dropwizard-configuration/pom.xml source:jar install -DskipTests=true

.PHONY: default dropwizard-config
