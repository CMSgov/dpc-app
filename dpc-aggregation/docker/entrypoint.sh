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

JAVA_CLASSES="-cp /app/resources:/app/classes:/app/libs/* gov.cms.dpc.aggregation.DPCAggregationService"

if [ -n "$NEW_RELIC_LICENSE_KEY" ]; then
    NR_AGENT="-javaagent:/newrelic/newrelic.jar"
else
    NR_AGENT=""
fi

if [ $DB_MIGRATION -eq 1 ]; then
  echo "Migrating the database"
  eval java ${JVM_FLAGS} ${JAVA_CLASSES} db migrate
fi

CMDLINE="java ${JVM_FLAGS} ${JACOCO} ${NR_AGENT} ${JAVA_CLASSES}"

echo "Running server via entrypoint!"

if [ -n "$JACOCO" ]; then
  exec ${CMDLINE} "$@"
else
  exec ${CMDLINE} "$@" 2>&1 | tee -a /var/log/dpc-aggregation-$(hostname).log
fi
