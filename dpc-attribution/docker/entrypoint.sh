#!/usr/bin/env sh

set -e

CMDLINE="java -cp /app/resources:/app/classes:/app/libs/* gov.cms.dpc.attribution.DPCAttributionService"

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
