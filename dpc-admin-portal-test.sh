#!/bin/bash
IS_AWS_EC2=$(./ops/scripts/is_aws_ec2.sh);
if [ "$IS_AWS_EC2" == "no" ]; then
  LOCAL_DOCKER_OVERRIDE="-f docker-compose.override.yml";
fi
PROJECT_NAME="${ADMIN_PROJ_NAME:-start-v1-portals}";

set -e

function _finally {
  docker compose -p $PROJECT_NAME down
  docker volume rm "$PROJECT_NAME"_pgdata16
  echo "^^^^^^^^^^^^^^^"
  echo "└└└└└└└└└└└└└└└-------- this volume has been removed!"
}

trap _finally EXIT

echo "┌───────────────────────┐"
echo "│                       │"
echo "│  Running Admin Tests  |"
  if [ "$IS_AWS_EC2" == "yes" ]; then
    echo "│       (AWS EC2)       │"
  else
    echo "│                       │"
  fi
echo "└───────────────────────┘"

# Build the container
make admin

# Prepare the environment 
DOCKER_PROJECT_NAME=$PROJECT_NAME make start-portal-dbs
docker compose -p $PROJECT_NAME -f docker-compose.yml $LOCAL_DOCKER_OVERRIDE -f docker-compose.portals.yml run --rm --entrypoint "bundle exec rails db:create db:migrate RAILS_ENV=test" dpc_web

# Run the tests
echo "┌───────────────────────────┐"
echo "│                           │"
echo "│  Running DPC Admin Tests  │"
echo "│                           │"
echo "└───────────────────────────┘"
docker compose -p $PROJECT_NAME -f docker-compose.yml $LOCAL_DOCKER_OVERRIDE -f docker-compose.portals.yml run --rm --entrypoint "bundle exec rubocop" dpc_admin
docker compose -p $PROJECT_NAME -f docker-compose.yml $LOCAL_DOCKER_OVERRIDE -f docker-compose.portals.yml run --rm --entrypoint "bundle exec rspec" dpc_admin

echo "┌────────────────────────────────┐"
echo "│                                │"
echo "│      Admin Tests Complete      │"
echo "│                                │"
echo "└────────────────────────────────┘"
