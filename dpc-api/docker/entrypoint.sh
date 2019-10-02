#!/usr/bin/env sh

set -e

if [ -n "$JACOCO" ]; then
  JACOCO="-javaagent:/org.jacoco.agent-runtime.jar=destfile=/jacoco-report/jacoco-it.exec"
else
  JACOCO=""
fi

CMDLINE="java ${JACOCO} -cp /app/resources:/app/classes:/app/libs/* gov.cms.dpc.api.DPCAPIService"

echo "Running server via entrypoint!"

if [ $DB_MIGRATION -eq 1 ]; then
  echo "Migrating the database"
  eval ${CMDLINE} db migrate
fi

exec ${CMDLINE} "$@"
