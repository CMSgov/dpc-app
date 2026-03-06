#!/bin/bash
set -e

echo "┌───────────────────────┐"
echo "│                       │"
echo "│ Running Api Gem Tests |"
echo "│                       │"
echo "└───────────────────────┘"

# Build the container
docker compose -f docker-compose.api-client.yml build dpc_api_client

# Run the tests
echo "┌───────────────────────────┐"
echo "│                           │"
echo "│  Running Api Gem Tests    │"
echo "│                           │"
echo "└───────────────────────────┘"
docker run --rm -v ${PWD}/engines/api_client/coverage:/api-client/coverage dpc-api-client bundle exec rspec
docker run --rm dpc-api-client bundle exec rubocop

echo "┌────────────────────────────────┐"
echo "│                                │"
echo "│     Api Gem Tests Complete     │"
echo "│                                │"
echo "└────────────────────────────────┘"
