#!/bin/bash
set -e

echo "┌───────────────────────┐"
echo "│                       │"
echo "│ Running Impl &        |"
echo "|   AdminV2 Tests       │"
echo "│                       │"
echo "└───────────────────────┘"

# Build the container
make impl
make adminv2

# Prepare the environment 
docker-compose -p start-v2-portals -f docker-compose.yml -f docker-compose.portals.yml up start_core_dependencies
docker-compose -p start-v2-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rails db:create db:migrate RAILS_ENV=test" dpc_impl

# Run the tests
echo "┌──────────────────────────┐"
echo "│                          │"
echo "│  Running DPC Impl Tests  │"
echo "│                          │"
echo "└──────────────────────────┘"
docker-compose -p start-v2-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rails spec" dpc_impl

echo "┌─────────────────────────────┐"
echo "│                             │"
echo "│  Running DPC AdminV2 Tests  │"
echo "│                             │"
echo "└─────────────────────────────┘"
docker-compose -p start-v2-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rails spec" dpc_adminv2

echo "┌──────────────────────────────────────────┐"
echo "│                                          │"
echo "│    DPC v2 Impl & Admin Tests Complete    │"
echo "│                                          │"
echo "└──────────────────────────────────────────┘"
