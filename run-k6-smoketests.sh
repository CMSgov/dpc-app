#!/bin/bash
set -Ee

if [[ "$*" == *"--k6-env=local"* ]]; then
  PORTAL_HOST="http://localhost:3100"
  WEB_ADMIN_HOST="http://localhost:3000"
  WEB_HOST="http://localhost:3900"
  API_ADMIN_URL="http://localhost:9903"
  API_METADATA_URL="http://localhost:3002/api/v1"
else
  API_ADMIN_URL=${ELB_URL}
fi

echo "running k6 frontend + backend scenarios for k6 smoketests"
k6 run \
  -e PORTAL_HOST=${PORTAL_HOST} \
  -e WEB_ADMIN_HOST=${WEB_ADMIN_HOST} \
  -e WEB_HOST=${WEB_HOST} \
  -e API_ADMIN_URL=${API_ADMIN_URL} \
  -e API_METADATA_URL=${API_METADATA_URL} \
  ./dpc-load-testing/smoketest-ci-app.js

echo "Done!"
