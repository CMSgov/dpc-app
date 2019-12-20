#!/usr/bin/env sh

set -e

if [ -n "$JACOCO" ]; then
  JACOCO="-javaagent:/org.jacoco.agent-runtime.jar=destfile=/jacoco-report/jacoco-it.exec"
else
  JACOCO=""
fi

CMDLINE="java $JVM_FLAGS ${JACOCO} -cp /app/resources:/app/classes:/app/libs/* gov.cms.dpc.attribution.DPCAttributionService"

if [ $DB_MIGRATION -eq 1 ]; then
  echo "Migrating the database"
  eval ${CMDLINE} db migrate
fi

if [ -n "$SEED" ]; then
    echo "Loading seeds"
    eval ${CMDLINE} seed
fi

echo "Running server"

exec ${CMDLINE} "$@" 2>&1 | tee -a /var/log/dpc-attribution-$(hostname).log
