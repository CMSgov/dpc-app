#!/bin/bash
set -Ee

# Current working directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

# Configure the Maven log level
export MAVEN_OPTS=-Dorg.slf4j.simpleLogger.defaultLogLevel=info

# Include secure environment variables
set -o allexport
[[ -f ${DIR}/ops/config/decrypted/local.env ]] && source ${DIR}/ops/config/decrypted/local.env
set +o allexport

function _finally {
  docker-compose down
}
trap _finally EXIT

docker-compose up start_core_dependencies
docker-compose up start_api_dependencies

# Run the integration tests
docker-compose up --exit-code-from tests tests

docker-compose down
