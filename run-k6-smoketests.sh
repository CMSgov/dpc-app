echo "starting docker command for k6"
docker run --rm -v $(pwd)/dpc-load-testing:/src --env-file $(pwd)/ops/config/decrypted/local.env --add-host host.docker.internal=host-gateway -e PORTAL_HOST=http://localhost:3100 -e WEB_ADMIN_HOST=http://localhost:3000 -e WEB_HOST=http://localhost:3900 -e ENVIRONMENT=local -i grafana/k6 run /src/smoketest-ci-app.js
echo "Done!"
