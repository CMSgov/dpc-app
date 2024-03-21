#!/usr/bin/env sh

set -e

# Clean the existing pid if the server was previously started (using docker-compose for example)
if [ -f tmp/pids/server.pid ]; then
  rm tmp/pids/server.pid
fi

if [ "$1" == "admin" ]; then
  # Start the database service (and make accessible outside the Docker container)
  echo "Starting Rails server..."
  bundle exec rails server -b 0.0.0.0 -p 3000
fi

if [ "$1" == "sidekiq" ]; then
  # Start Sidekiq job processing
  bundle exec sidekiq -q admin
fi
