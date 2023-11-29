#!/usr/bin/env sh

set -e

# Clean the existing pid if the server was previously started (using docker-compose for example)
if [ -f tmp/pids/server.pid ]; then
  rm tmp/pids/server.pid
fi

if [ "$1" == "web" ]; then
  # Run the database migrations
  echo "Migrating the database..."
  bundle exec rails db:migrate

  # Seed the database
  # This step is not needed, as there is no database seed data yet

  # Start the database service (and make accessible outside the Docker container)
  echo "Starting Rails server..."
  if [[ -n "$JACOCO" ]]; then
    bundle exec rails server -b 0.0.0.0 -p 3500
  else
    bundle exec rails server -b 0.0.0.0 -p 3500 2>&1 | tee -a /var/log/dpc-web-$(hostname).log
  fi
fi

if [ "$1" == "sidekiq" ]; then
  # Start Sidekiq job processing
  if [[ -n "$JACOCO" ]]; then
    bundle exec sidekiq -q web
  else
    bundle exec sidekiq -q web 2>&1 | tee -a /var/log/dpc-web-$(hostname)-sidekiq.log
  fi
fi
