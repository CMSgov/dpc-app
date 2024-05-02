#!/usr/bin/env sh

set -e

# Clean the existing pid if the server was previously started (using docker-compose for example)
if [ -f tmp/pids/server.pid ]; then
  rm tmp/pids/server.pid
fi

if [ "$1" == "portal" ]; then
  # Start the database service (and make accessible outside the Docker container)
  echo "Starting Rails server..."

  echo "Migrating the database..."
  bundle exec rails db:migrate

  if [[ "$ENV" == "production" ]]; then
    echo "Starting in production"
    bundle exec rails server -b 0.0.0.0 -p 3100
  elif [[ "$ENV" == "local" ]]; then
    echo "Starting in development"
    ./bin/dev
  else
    echo "Starting in non-production"
    ./bin/nonprod
  fi
fi

if [ "$1" == "sidekiq" ]; then
  # Start Sidekiq job processing
  if [[ "$ENV" == "local" ]]; then
    echo "Starting in development"
    ./bin/sidekiq-dev
  else
    echo "Starting in non-local"
    bundle exec sidekiq -q portal
  fi
fi
