#!/usr/bin/env sh

set -e

# Clean the existing pid if the server was previously started (using docker-compose for example)
if [ -f tmp/pids/server.pid ]; then
  rm tmp/pids/server.pid
fi

if [ "$1" == "impl" ]; then
  # Run the database migrations
  echo "Migrating the database..."
  bundle exec rails db:migrate

  # Start the database service (and make accessible outside the Docker container)
  echo "Starting Rails server..."
  if [[ -n "$JACOCO" ]]; then
    bundle exec rails server -b 0.0.0.0 -p 3000
  else
    bundle exec rails server -b 0.0.0.0 -p 3000 2>&1 | tee -a /var/log/dpc-impl-$(hostname).log
  fi
fi

if [ "$1" == "sidekiq" ]; then
  # Start Sidekiq job processing
  if [[ -n "$JACOCO" ]]; then
    bundle exec sidekiq -q default -q mailers
  else
    bundle exec sidekiq -q default -q mailers 2>&1 | tee -a /var/log/dpc-impl-$(hostname)-sidekiq.log
  fi
fi
