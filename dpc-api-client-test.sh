#!/bin/bash
set -e

echo "┌───────────────────────┐"
echo "│                       │"
echo "│ Running Api Gem Tests |"
echo "│                       │"
echo "└───────────────────────┘"

# Build the container
docker build -f engines/api_client/Dockerfile engines/api_client -t api-client

# Run the tests
echo "┌───────────────────────────┐"
echo "│                           │"
echo "│  Running Api Gem Tests    │"
echo "│                           │"
echo "└───────────────────────────┘"
docker run --rm -v ${PWD}/engines/api_client/coverage:/api-client/coverage api-client bundle exec rspec
docker run --rm api-client bundle exec rubocop

echo "┌────────────────────────────────┐"
echo "│                                │"
echo "│     Api Gem Tests Complete     │"
echo "│                                │"
echo "└────────────────────────────────┘"
