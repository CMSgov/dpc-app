#!/bin/bash
set -e

function _finally {
    docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml down
    docker volume rm start-v1-portals_pgdata16
}
trap _finally EXIT

echo "┌───────────────────────┐"
echo "│                       │"
echo "│   Running Web Tests   |"
echo "│                       │"
echo "└───────────────────────┘"

# Build the container
make website

# Prepare the environment 
docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml up db --wait
docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rails db:create db:migrate RAILS_ENV=test" web_sidekiq

# Run the tests
echo "┌─────────────────────────┐"
echo "│                         │"
echo "│  Running DPC Web Tests  │"
echo "│                         |"
echo "└─────────────────────────┘"
docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rubocop" web_sidekiq
docker compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rspec" web_sidekiq

echo "┌──────────────────────────────┐"
echo "│                              │"
echo "│      Web Tests Complete      │"
echo "│                              │"
echo "└──────────────────────────────┘"
