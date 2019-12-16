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
docker-compose -f dpc-web/docker-compose.yml run web bundle exec rails db:create db:migrate db:seed
docker-compose -f dpc-web/docker-compose.yml run web bundle exec rails spec

# Run bundler audit (wrap all of this up as a shell command, otherwise we get weird PATH issues
docker-compose -f dpc-web/docker-compose.yml run web sh -c "cd /dpc-web && bundle exec bundle audit update && bundle exec bundle audit check --ignore CVE-2015-9284"

echo "┌──────────────────────────────────────────┐"
echo "│                                          │"
echo "│    All Website Tests & Audit Complete    │"
echo "│                                          │"
echo "└──────────────────────────────────────────┘"
