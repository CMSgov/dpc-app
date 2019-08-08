#!/usr/bin/env sh

set -e

if [ -n "$JACOCO" ]; then
  JACOCO="-javaagent:/org.jacoco.agent-runtime.jar=destfile=/jacoco-report/jacoco-it.exec"
else
  JACOCO=""
fi

CMDLINE="java ${JACOCO} -cp /app/resources:/app/classes:/app/libs/* gov.cms.dpc.attribution.DPCAttributionService"

if [ -n "$SEED" ]; then
    echo "Loading seeds"
    ${CMDLINE} db migrate
    ${CMDLINE} seed
fi

echo "Running server"

exec ${CMDLINE} "$@"
