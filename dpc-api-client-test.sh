#!/bin/bash
set -e

echo "┌───────────────────────┐"
echo "│                       │"
echo "│ Running Api Gem Tests |"
echo "│                       │"
echo "└───────────────────────┘"

# Build the container
docker compose -f  docker-compose.dpc-client.yml build dpc_client

# Run the tests
echo "┌───────────────────────────┐"
echo "│                           │"
echo "│  Running Api Gem Tests    │"
echo "│                           │"
echo "└───────────────────────────┘"
docker run --rm -v ${PWD}/engines/api_client/coverage:/api-client/coverage dpc-client bundle exec rspec
docker run --rm dpc-client bundle exec rubocop

echo "┌────────────────────────────────┐"
echo "│                                │"
echo "│     Api Gem Tests Complete     │"
echo "│                                │"
echo "└────────────────────────────────┘"
