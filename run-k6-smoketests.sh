#!/bin/bash
set -Ee

if [[ "$*" == *"--k6-env=local"* ]]; then
  echo "running local!!!"
  PORTAL_HOST="http://localhost:3100"
  WEB_ADMIN_HOST="http://localhost:3000"
  WEB_HOST="http://localhost:3900"
  API_ADMIN_URL="http://localhost:9903"
  K6_ENVIRONMENT="local"
else
  # Uses ELB_URL if exported; otherwise the literal fallback "ELB_URL"
  echo "running remote setup!!!"
  echo "running remote setup!!!"
  echo "running remote setup!!!"
  echo "running remote setup!!!"
  API_ADMIN_URL=ELB_URL
  K6_ENVIRONMENT="prod"
fi

echo "starting k6"
echo "running k6 frontend tests"
k6 run dpc-load-testing/smoketest-frontend-ci-app.js \
  -e PORTAL_HOST=${PORTAL_HOST} \
  -e WEB_ADMIN_HOST=${WEB_ADMIN_HOST} \
  -e WEB_HOST=${WEB_HOST} \

echo "running (authenticated) backend tests in docker"
docker run --rm -v $(pwd)/dpc-load-testing:/src --env-file $(pwd)/ops/config/decrypted/local.env \
  --add-host host.docker.internal=host-gateway \
  -e PORTAL_HOST=${PORTAL_HOST} \
  -e WEB_ADMIN_HOST=${WEB_ADMIN_HOST} \
  -e WEB_HOST=${WEB_HOST} \
  -e API_ADMIN_URL=${API_ADMIN_URL} \
  -e ENVIRONMENT=${K6_ENVIRONMENT} -i grafana/k6 run /src/smoketest-backend-ci-app.js

echo "Done!"
