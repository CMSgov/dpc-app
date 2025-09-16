#!/usr/bin/env sh

set -e

bootstrap_config() {
  # Create the config directory
  mkdir -p /config

  # Sync the aws bucket
  bucket=$(aws ssm get-parameter --name /dpc/"$ENV"/config_bucket --query Parameter.Value --output text)
  aws s3 sync "s3://$bucket" config/
}

if [ -n "$JACOCO" ]; then
  JACOCO="-javaagent:/org.jacoco.agent-runtime.jar=destfile=/jacoco-report/jacoco-it.exec"
else
  JACOCO=""
fi

if [ -n "$BOOTSTRAP" ]; then
  echo "Bootstrapping image from S3"
  bootstrap_config
fi

JAVA_CLASSES="-cp /app/resources:/app/classes:/app/libs/* gov.cms.dpc.aggregation.DPCAggregationService"

if [ -n "$NEW_RELIC_LICENSE_KEY" ]; then
  NR_AGENT="-javaagent:/newrelic/newrelic.jar"
else
  NR_AGENT=""
fi

# set env vars for Dropwizard application
set -o allexport
ENV_FILE="/app/resources/${ENV}.application.env"
echo "Exporting env file ${ENV_FILE}"
# shellcheck source=/dev/null
. "$ENV_FILE"
set +o allexport

CONF_FILE="/app/resources/application.yml"

if [ $DB_MIGRATION -eq 1 ]; then
  echo "Migrating the databases"
  eval java ${JAVA_CLASSES} queuedb migrate ${CONF_FILE} --migrations "/app/resources/migrations/queue.migrations.xml"
  eval java ${JAVA_CLASSES} consentdb migrate ${CONF_FILE} --migrations "/app/resources/consent.migrations.xml"
fi

if [ "$DEBUG_MODE" = "true" ]; then
  echo "Setting debug mode"
  DEBUG_FLAGS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
else
  DEBUG_FLAGS=""
fi

CMDLINE="java ${DEBUG_FLAGS} ${JACOCO} ${NR_AGENT} ${JAVA_CLASSES}"

echo "Running server via entrypoint!"
exec ${CMDLINE} "$@" ${CONF_FILE}
