#!/usr/bin/env sh

set -e

bootstrap_config() {
  # Create the config directory
  mkdir -p /config

  # Sync the aws bucket
  aws s3 sync s3://dpc-$ENV-app-config/ config/
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

JAVA_CLASSES="-cp /app/resources:/app/classes:/app/libs/* gov.cms.dpc.api.DPCAPIService"

# If we have NewRelic license, enable the java agent.
if [ -n "$NEW_RELIC_LICENSE_KEY" ]; then
    NR_AGENT="-javaagent:/newrelic/newrelic.jar"
else
    NR_AGENT=""
fi

set -o allexport
# shellcheck source=/dev/null
. "/app/resources/${ENV:-local}.application.conf"
set +o allexport

CONF_FILE="/app/resources/ci.application.yml"

if [ $DB_MIGRATION -eq 1 ]; then
  echo "Migrating the database"
  eval "java ${CONF_FLAGS} ${JAVA_CLASSES} db migrate ${CONF_FILE}"
fi

if [ "$DEBUG_MODE" = "true" ]; then
    echo "Setting debug mode"
    DEBUG_FLAGS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
else
    DEBUG_FLAGS=""
fi

CMDLINE="java ${CONF_FLAGS} ${DEBUG_FLAGS} ${JACOCO} ${NR_AGENT} ${JAVA_CLASSES}"

echo "Running server via entrypoint!"

if [ -n "$JACOCO" ]; then
  exec ${CMDLINE} "$@" ${CONF_FILE}
else
  exec ${CMDLINE} "$@" ${CONF_FILE} 2>&1 | tee -a /var/log/dpc-api-$(hostname).log
fi
