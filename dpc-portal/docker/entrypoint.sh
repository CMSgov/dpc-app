#!/usr/bin/env sh

set -e

# Clean the existing pid if the server was previously started (using docker-compose for example)
if [ -f tmp/pids/server.pid ]; then
  rm tmp/pids/server.pid
fi

if [ "$1" == "portal" ]; then
  # Start the database service (and make accessible outside the Docker container)
  echo "Starting Rails server..."
  if [[ -n "$JACOCO" ]]; then
    bundle exec rails server -b 0.0.0.0 -p 3100
  else
    bundle exec rails server -b 0.0.0.0 -p 3100 2>&1 | tee -a /var/log/dpc-portal-$(hostname).log
  fi
fi

if [ "$1" == "sidekiq" ]; then
  # Start Sidekiq job processing
  if [[ -n "$JACOCO" ]]; then
    bundle exec sidekiq -q portal
  else
    bundle exec sidekiq -q portal 2>&1 | tee -a /var/log/dpc-portal-$(hostname)-sidekiq.log
  fi
fi
