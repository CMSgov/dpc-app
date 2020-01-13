#!/usr/bin/env sh

set -e

if [ -n "$JACOCO" ]; then
  JACOCO="-javaagent:/org.jacoco.agent-runtime.jar=destfile=/jacoco-report/jacoco-it.exec"
else
  JACOCO=""
fi

if [ -n "$NEW_RELIC_LICENSE_KEY" ]; then
    CMDLINE="java -javaagent:/newrelic/newrelic.jar $JVM_FLAGS ${JACOCO} -cp /app/resources:/app/classes:/app/libs/* gov.cms.dpc.consent.DPCConsentService"
else
    CMDLINE="java $JVM_FLAGS ${JACOCO} -cp /app/resources:/app/classes:/app/libs/* gov.cms.dpc.consent.DPCConsentService"
fi

if [ $DB_MIGRATION -eq 1 ]; then
  echo "Migrating the database"
  eval ${CMDLINE} db migrate
fi

echo "Running server"

if [ -n "$JACOCO" ]
  exec ${CMDLINE} "$@"
else
  exec ${CMDLINE} "$@" 2>&1 | tee -a /var/log/dpc-consent-$(hostname).log
fi
