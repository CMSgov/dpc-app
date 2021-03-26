#!/bin/bash
set -e

echo "┌─────────────────────────┐"
echo "│                         │"
echo "│  Running Admin   Tests  │"
echo "│                         │"
echo "└─────────────────────────┘"

# Build the container
make admin

# Run the tests
docker-compose -f docker-compose.portals.yml up start_core_dependencies
docker-compose -f docker-compose.portals.yml run --entrypoint "bundle exec rails spec" dpc_admin

echo "┌──────────────────────────────────┐"
echo "│                                  │"
echo "│    All Admin Tests   Complete    │"
echo "│                                  │"
echo "└──────────────────────────────────┘"
