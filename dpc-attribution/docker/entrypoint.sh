#!/usr/bin/env sh

set -e

if [ -n "$JACOCO" ]; then
  JACOCO="-javaagent:/org.jacoco.agent-runtime.jar=destfile=/jacoco-report/jacoco-it.exec"
else
  JACOCO=""
fi

JAVA_CLASSES="-cp /app/resources:/app/classes:/app/libs/* gov.cms.dpc.attribution.DPCAttributionService"

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
  echo "Migrating the database"
  eval java ${JAVA_CLASSES} db migrate ${CONF_FILE}
fi

if [ "$DEBUG_MODE" = "true" ]; then
  echo "Setting debug mode"
  DEBUG_FLAGS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
else
  DEBUG_FLAGS=""
fi

CMDLINE="java ${DEBUG_FLAGS} ${JACOCO} ${NR_AGENT} ${JAVA_CLASSES}"

if [ -n "$SEED" ]; then
  echo "Loading seeds"
  eval java ${JVM_FLAGS} ${JAVA_CLASSES} seed ${CONF_FILE}
fi

# Make sure volumes in our persisted environments are writeable by nobody
if [ -d "/tmp" ]; then chown nobody:nobody /tmp; fi
if [ -d "/newrelic/logs" ]; then chown nobody:nobody /newrelic/logs; fi

echo "Running server via entrypoint as nobody!"
# Note: -E preserves "most" env variables, but not all.  Ones deemed sensitive, like ENV need to be passed explicitly.
exec sudo -E ENV=$ENV -u nobody ${CMDLINE} "$@" ${CONF_FILE}
