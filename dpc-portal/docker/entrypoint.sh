#!/usr/bin/env sh

set -e

# Clean the existing pid if the server was previously started (using docker-compose for example)
if [ -f tmp/pids/server.pid ]; then
  rm tmp/pids/server.pid
fi
if [ -f tmp/solid_queue_pidfile ]; then
  echo "Removing solid queue pidfile"
  rm tmp/solid_queue_pidfile
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
elif [ "$1" == "async" ]; then
  echo "Starting Solid Queue..."
  if [[ "$ENV" == "production" ]]; then
    echo "Starting in production"
    bundle exec rails solid_queue:start
  # For local, SolidQueue starts in same container
  elif [[ "$ENV" != "local" ]]; then
    echo "Starting in non-production"
    ./bin/nonprod_async
  fi
fi
