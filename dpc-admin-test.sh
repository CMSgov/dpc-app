#!/bin/bash
set -e

echo "┌─────────────────────────┐"
echo "│                         │"
echo "│  Running Website Tests  │"
echo "│                         │"
echo "└─────────────────────────┘"

# Build the container
make admin

# Run the tests
docker-compose -f dpc-admin/docker-compose.yml up start_core_dependencies
docker-compose -f dpc-admin/docker-compose.yml run --entrypoint "bundle exec rails db:create db:migrate RAILS_ENV=test" dpc_admin
docker-compose -f dpc-admin/docker-compose.yml run --entrypoint "bundle exec rails spec" dpc_admin

echo "┌──────────────────────────────────┐"
echo "│                                  │"
echo "│    All Website Tests Complete    │"
echo "│                                  │"
echo "└──────────────────────────────────┘"
