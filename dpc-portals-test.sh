#!/bin/bash
set -e

echo "┌───────────────────────┐"
echo "│                       │"
echo "│ Running Web, Admin,   |"
echo "| Impl, & AdminV2 Tests │"
echo "│                       │"
echo "└───────────────────────┘"

# Build the container
make website
make admin
make impl
make adminv2

# Prepare the environment 
docker-compose -p start-v2-portals -f docker-compose.yml -f docker-compose.portals.yml up start_core_dependencies
docker-compose -p start-v2-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rails db:create db:migrate RAILS_ENV=test" dpc_web
docker-compose -p start-v2-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rails db:create db:migrate RAILS_ENV=test" dpc_impl

# Run the tests
echo "┌─────────────────────────┐"
echo "│                         │"
echo "│  Running DPC Web Tests  │"
echo "│                         │"
echo "└─────────────────────────┘"
docker-compose -p start-v2-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "rubocop" dpc_web
docker-compose -p start-v2-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rails spec" dpc_web

echo "┌───────────────────────────┐"
echo "│                           │"
echo "│  Running DPC Admin Tests  │"
echo "│                           │"
echo "└───────────────────────────┘"
docker-compose -p start-v2-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "rubocop" dpc_admin
docker-compose -p start-v2-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rails spec" dpc_admin

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
echo "│    All Website & Admin Tests Complete    │"
echo "│                                          │"
echo "└──────────────────────────────────────────┘"
