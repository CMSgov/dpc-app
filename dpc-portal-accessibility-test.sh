#!/bin/bash
set -e

PROJECT_NAME="start-v1-portals"

function _finally {
  docker compose -p $PROJECT_NAME down
  docker volume rm "$PROJECT_NAME"_pgdata16
  echo "^^^^^^^^^^^^^^^"
  echo "└└└└└└└└└└└└└└└-------- this volume has been removed!"
}
trap _finally EXIT

echo "┌──────────────---───────--------------──┐"
echo "│                                        │"
echo "│ Setting up Portal Accessibility Tests  |"
echo "│                                        │"
echo "└───────────────────-----------------────┘"

# Build the container
make portal

# Prepare the environment 
DOCKER_PROJECT_NAME=$PROJECT_NAME make start-portal-dbs
docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rails db:create db:migrate RAILS_ENV=test" dpc_portal

# Run the tests
echo "┌──────────────---───────--------------──┐"
echo "│                                        │"
echo "│   Running Portal Accessibility Tests   |"
echo "│                                        │"
echo "└──-─────────────────────────────-------─┘"

docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint docker/accessibility-test.sh dpc_portal

echo "┌──────────────---───────--------------──┐"
echo "│                                        │"
echo "│  Portal Accessibility Tests Complete   │"
echo "│                                        │"
echo "└──-─────────────────────────────-------─┘"
