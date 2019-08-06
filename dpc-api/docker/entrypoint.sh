#!/usr/bin/env sh

set -e

CMDLINE="java  -cp /app/resources:/app/classes:/app/libs/* gov.cms.dpc.api.DPCAPIService"
#-javaagent:/jacoco-agent/org.jacoco.agent-runtime.jar=destfile=/jacoco-report/jacoco-it.exec

echo "Running server via entrypoint!"

exec ${CMDLINE} "$@"
