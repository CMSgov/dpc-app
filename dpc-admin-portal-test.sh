#!/bin/bash
set -e

function _finally {
    docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml down
    docker volume rm start-v1-portals_pgdata16
}
trap _finally EXIT

echo "┌───────────────────────┐"
echo "│                       │"
echo "│ Running Admin Tests   |"
echo "│                       │"
echo "└───────────────────────┘"

# Build the container
make admin

# Prepare the environment 
docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml up db --wait
docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rails db:create db:migrate RAILS_ENV=test" web_sidekiq

# Run the tests
echo "┌───────────────────────────┐"
echo "│                           │"
echo "│  Running DPC Admin Tests  │"
echo "│                           │"
echo "└───────────────────────────┘"
docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rubocop" admin_sidekiq
docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rspec" admin_sidekiq

echo "┌────────────────────────────────┐"
echo "│                                │"
echo "│      Admin Tests Complete      │"
echo "│                                │"
echo "└────────────────────────────────┘"
