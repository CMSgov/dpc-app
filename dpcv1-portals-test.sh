#!/bin/bash
set -e

echo "┌───────────────────────┐"
echo "│                       │"
echo "│ Running Web & Admin   |"
echo "|      Tests            │"
echo "│                       │"
echo "└───────────────────────┘"

# Build the container
make website
make admin

# Prepare the environment 
docker-compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml up start_core_dependencies
docker-compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rails db:create db:migrate RAILS_ENV=test" dpc_web
docker-compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rails db:create db:migrate RAILS_ENV=test" dpc_portal

# Run the tests
echo "┌─────────────────────────┐"
echo "│                         │"
echo "│  Running DPC Web Tests  │"
echo "│                         │"
echo "└─────────────────────────┘"
docker-compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rubocop" dpc_web
docker-compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rspec" dpc_web

echo "┌───────────────────────────┐"
echo "│                           │"
echo "│  Running DPC Admin Tests  │"
echo "│                           │"
echo "└───────────────────────────┘"
docker-compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rubocop" dpc_admin
docker-compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rspec" dpc_admin

echo "┌───────────────────────────┐"
echo "│                           │"
echo "│  Running DPC Portal Tests │"
echo "│                           │"
echo "└───────────────────────────┘"
docker-compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rubocop" dpc_portal
docker-compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rspec" dpc_portal

echo "┌──────────────────────────────────────────┐"
echo "│                                          │"
echo "│      Website & Admin Tests Complete      │"
echo "│                                          │"
echo "└──────────────────────────────────────────┘"
