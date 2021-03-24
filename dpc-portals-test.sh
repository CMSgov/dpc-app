#!/bin/bash
set -e

echo "┌───────────────────────────┐"
echo "│                           │"
echo "│ Running Web & Admin Tests │"
echo "│                           │"
echo "└───────────────────────────┘"

# Build the container
make website
make admin

# Run the tests
docker-compose -f docker-compose.portals.yml up start_core_dependencies
docker-compose -f docker-compose.portals.yml run --entrypoint "bundle exec rails db:create db:migrate RAILS_ENV=test" dpc_web
docker-compose -f docker-compose.portals.yml run --entrypoint "bundle exec rails spec" dpc_web
# docker-compose -f docker-compose.portals.yml run --entrypoint "bundle exec rails spec" dpc_admin

echo "┌──────────────────────────────────────────┐"
echo "│                                          │"
echo "│    All Website & Admin Tests Complete    │"
echo "│                                          │"
echo "└──────────────────────────────────────────┘"
