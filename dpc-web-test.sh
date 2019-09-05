#!/bin/bash
set -e

echo "┌──────────────────────────────────────────┐"
echo "│                                          │"
echo "│  Running Website Tests & Bundler Aduit   │"
echo "│                                          │"
echo "└──────────────────────────────────────────┘"

# Build the container
make website

# Run the tests
docker-compose -f dpc-web/docker-compose.yml up start_core_dependencies
docker-compose -f dpc-web/docker-compose.yml run web rails db:create db:migrate db:seed
docker-compose -f dpc-web/docker-compose.yml run web rails spec

echo "┌──────────────────────────────────────────┐"
echo "│                                          │"
echo "│     All Website Tests & AuditComplete    │"
echo "│                                          │"
echo "└──────────────────────────────────────────┘"