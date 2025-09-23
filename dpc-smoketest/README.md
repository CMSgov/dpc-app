# DPC Smoke Tests
A set of smoke tests to cover different system components and ensure new builds run correctly across all environments.
```dtd
docker run --rm -v $(pwd)/dpc-smoketest:/src --env-file $(pwd)/ops/config/decrypted/local.env --add-host host.docker.internal=host-gateway -e PORTAL_HOST=http://localhost:3100 -e WEB_ADMIN_HOST=http://localhost:3000 -e WEB_HOST=http://localhost:3900  -i grafana/k6 run /src/ci-app.js
```
