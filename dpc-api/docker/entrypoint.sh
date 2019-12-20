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

CMDLINE="java $JVM_FLAGS ${JACOCO} -cp /app/resources:/app/classes:/app/libs/* gov.cms.dpc.api.DPCAPIService"

if [ $DB_MIGRATION -eq 1 ]; then
  echo "Migrating the database"
  eval ${CMDLINE} db migrate
fi

echo "Running server via entrypoint!"

exec ${CMDLINE} "$@" 2>&1 | tee -a /var/log/$(hostname).log
