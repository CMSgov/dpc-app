#!/bin/bash
set -Ee


if [[ "$*" == *"--k6-env=local"* ]]; then
  PORTAL_HOST="http://localhost:3100"
  WEB_HOST="http://localhost:3900"
  WEB_ADMIN_HOST="http://localhost:3000"
  ENV="local"

  echo "running frontend smoketests against ${HOST_URL}"
  docker run --rm -v $(pwd)/dpc-load-testing:/src --env-file $(pwd)/ops/config/decrypted/local.env \
    --network=host \
    -e PORTAL_HOST=${PORTAL_HOST} \
    -e WEB_HOST=${WEB_HOST} \
    -e WEB_ADMIN_HOST=${WEB_ADMIN_HOST} \
    -e ENV=${ENV} \
    -i grafana/k6 run /src/smoketest-frontend-ci-app.js
  echo "Done!"
else
  echo "running frontend smoketests locally"
  k6 run \
    -e PORTAL_HOST=${PORTAL_HOST} \
    -e WEB_HOST=${WEB_HOST} \
    -e WEB_ADMIN_HOST=${WEB_ADMIN_HOST} \
    e ${ENV} \
    ./dpc-load-testing/smoketest-frontend-ci-app.js
fi

