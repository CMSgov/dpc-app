#!/bin/bash
set -Ee

if [[ "$*" == *"--k6-env=local"* ]]; then
  API_ADMIN_URL="http://host.docker.internal:9903"
  API_METADATA_URL="http://host.docker.internal:3002/api/v1"
  docker run --rm -v $(pwd)/dpc-load-testing:/src --env-file $(pwd)/ops/config/decrypted/local.env \
  --add-host host.docker.internal=host-gateway \
  -e API_ADMIN_URL=${API_ADMIN_URL} \
  -e API_METADATA_URL=${API_METADATA_URL} \
  -e ENV=local \
  -i grafana/k6 run /src/smoketest-backend-ci-app.js
else
  k6 run \
    -e API_ADMIN_URL=${API_ADMIN_URL} \
    -e API_METADATA_URL=${API_METADATA_URL} \
    -e ENV=${ENV} \
    ./dpc-load-testing/smoketest-backend-ci-app.js
fi

echo "K6 Backend Smoketests done!"
