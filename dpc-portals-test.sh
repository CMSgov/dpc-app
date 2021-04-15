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

# Prepare the environment 
docker-compose -p dpc-portals -f docker-compose.portals.yml up start_core_dependencies
docker-compose -p dpc-portals -f docker-compose.portals.yml run --entrypoint "bundle exec rails db:create db:migrate RAILS_ENV=test" dpc_web

# Run the tests
echo "┌─────────────────────────┐"
echo "│                         │"
echo "│  Running DPC Web Tests  │"
echo "│                         │"
echo "└─────────────────────────┘"
docker-compose -p dpc-portals -f docker-compose.portals.yml run --entrypoint "rubocop" dpc_web
docker-compose -p dpc-portals -f docker-compose.portals.yml run --entrypoint "bundle exec rails spec" dpc_web

echo "┌───────────────────────────┐"
echo "│                           │"
echo "│  Running DPC Admin Tests  │"
echo "│                           │"
echo "└───────────────────────────┘"
docker-compose -p dpc-portals -f docker-compose.portals.yml run --entrypoint "rubocop" dpc_admin
docker-compose -p dpc-portals -f docker-compose.portals.yml run --entrypoint "bundle exec rails spec" dpc_admin

echo "┌──────────────────────────────────────────┐"
echo "│                                          │"
echo "│    All Website & Admin Tests Complete    │"
echo "│                                          │"
echo "└──────────────────────────────────────────┘"
