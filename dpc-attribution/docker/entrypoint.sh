#!/usr/bin/env sh

set -e

JACOCO="-javaagent:/org.jacoco.agent-runtime.jar=destfile=/jacoco-report/jacoco-it.exec"

CMDLINE="java ${JACOCO} -cp /app/resources:/app/classes:/app/libs/* gov.cms.dpc.attribution.DPCAttributionService"

run_java() {
     "$@"
}

if [ -n "$SEED" ]; then
    echo "Loading seeds"
    ${CMDLINE} db migrate
    ${CMDLINE} seed
fi

echo "Running server"

exec ${CMDLINE} "$@"
