#!/bin/bash
set -e

function _finally {
    docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml down
    docker volume rm start-v1-portals_pgdata14
}
trap _finally EXIT

echo "┌───────────────────────┐"
echo "│                       │"
echo "│ Running Portal Tests  |"
echo "│                       │"
echo "└───────────────────────┘"

# Build the container
make portal

# Prepare the environment 
docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml up start_core_dependencies
docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rails db:create db:migrate RAILS_ENV=test" dpc_portal

# Run the tests
echo "┌───────────────────────────┐"
echo "│                           │"
echo "│  Running DPC Portal Tests │"
echo "│                           │"
echo "└───────────────────────────┘"
docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rubocop" dpc_portal
docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rspec" dpc_portal
docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint docker/accessibility-test.sh dpc_portal

echo "┌────────────────────────────────┐"
echo "│                                │"
echo "│      Portal Tests Complete     │"
echo "│                                │"
echo "└────────────────────────────────┘"
