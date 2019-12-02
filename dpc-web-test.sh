#!/bin/bash
set -e

echo "┌──────────────────────────────────────────┐"
echo "│                                          │"
echo "│  Running Website Tests & Bundler Audit   │"
echo "│                                          │"
echo "└──────────────────────────────────────────┘"

# Build the container
make website

# Run the tests
docker-compose -f dpc-web/docker-compose.yml up start_core_dependencies
docker-compose -f dpc-web/docker-compose.yml run web rails db:create db:migrate db:seed
docker-compose -f dpc-web/docker-compose.yml run web rails spec

# Run bundler audit
docker-compose -f dpc-web/docker-compose.yml run web bundle audit update && bundle audit check --ignore CVE-2015-9284

echo "┌──────────────────────────────────────────┐"
echo "│                                          │"
echo "│    All Website Tests & Audit Complete    │"
echo "│                                          │"
echo "└──────────────────────────────────────────┘"