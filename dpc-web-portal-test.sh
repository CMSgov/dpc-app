#!/bin/bash
set -e

echo "┌───────────────────────┐"
echo "│                       │"
echo "│   Running Web Tests   |"
echo "│                       │"
echo "└───────────────────────┘"

# Build the container
make website

# Prepare the environment 
docker-compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml up start_core_dependencies
docker-compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rails db:create db:migrate RAILS_ENV=test" dpc_web

# Run the tests
echo "┌─────────────────────────┐"
echo "│                         │"
echo "│  Running DPC Web Tests  │"
echo "│                         |"
echo "└─────────────────────────┘"
docker-compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rubocop" dpc_web
docker-compose -p start-v1-portals -f docker-compose.yml -f docker-compose.portals.yml run --entrypoint "bundle exec rspec" dpc_web

echo "┌──────────────────────────────┐"
echo "│                              │"
echo "│      Web Tests Complete      │"
echo "│                              │"
echo "└──────────────────────────────┘"
