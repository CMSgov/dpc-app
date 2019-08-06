#!/usr/bin/env sh

set -e

JACOCO="-javaagent:/org.jacoco.agent-runtime.jar=destfile=/jacoco-report/jacoco-it.exec"

CMDLINE="java ${JACOCO} -cp /app/resources:/app/classes:/app/libs/* gov.cms.dpc.aggregation.DPCAggregationService"


echo "Running server via entrypoint!"
exec ${CMDLINE} "$@"
