#!/usr/bin/env sh

set -e

CMDLINE="java -cp /app/resources:/app/classes:/app/libs/* gov.cms.dpc.api.DPCAPIService"


echo "Running server via entrypoint!"

exec ${CMDLINE} "$@"
