#!/bin/bash
set -e

function _finally {
    docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml down --remove-orphans
    docker volume rm start-v1-portals_pgdata16
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
docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml up db --wait
docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rails db:create db:migrate RAILS_ENV=test" dpc_portal

# Run the tests
echo "┌───────────────────────────┐"
echo "│                           │"
echo "│  Running DPC Portal Tests │"
echo "│                           │"
echo "└───────────────────────────┘"
docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rubocop" dpc_portal
docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rspec" dpc_portal

docker compose -f ./docker-compose.base.yml build base
mvn -T 1.5C clean compile -Perror-prone -B -V -ntp -DskipTests
mvn -T 1.5C package -Pci -ntp -DskipTests

USE_BFD_MOCK=true docker compose -p start-v1-portals up api --wait
GOLDEN_MACAROON=$(curl -X POST http://localhost:9903/tasks/generate-token) \
SKIP_SIMPLE_COV=true \
docker compose -p start-v1-portals \
-f docker-compose.yml -f docker-compose.portals.yml \
run --remove-orphans \
--entrypoint "bundle exec rspec --tag integration" \
dpc_portal


echo "┌────────────────────────────────┐"
echo "│                                │"
echo "│      Portal Tests Complete     │"
echo "│                                │"
echo "└────────────────────────────────┘"
