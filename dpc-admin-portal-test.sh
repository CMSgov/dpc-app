#!/bin/bash
set -e

echo "┌───────────────────────┐"
echo "│                       │"
echo "│ Running Admin Tests   |"
echo "│                       │"
echo "└───────────────────────┘"

# Build the container
make admin

# Prepare the environment 
docker-compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml up start_core_dependencies
docker-compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rails db:create db:migrate RAILS_ENV=test" dpc_web

# Run the tests
echo "┌───────────────────────────┐"
echo "│                           │"
echo "│  Running DPC Admin Tests  │"
echo "│                           │"
echo "└───────────────────────────┘"
docker-compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "rubocop" dpc_admin
docker-compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rails spec" dpc_admin

echo "┌────────────────────────────────┐"
echo "│                                │"
echo "│      Admin Tests Complete      │"
echo "│                                │"
echo "└────────────────────────────────┘"
