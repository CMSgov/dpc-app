#!/usr/bin/env sh

set -e

# Clean the existing pid if the server was previously started (using docker-compose for example)
if [ -f tmp/pids/server.pid ]; then
  rm tmp/pids/server.pid
fi

# Run the database migrations
echo "Migrating the database..."
bundle exec rails db:migrate

# Seed the database
# This step is not needed, as there is no database seed data yet

# Start background job processing
# TODO: We should avoid using the dameonize flag in production and, instead, pursue a deployment
# like provided here https://github.com/mperham/sidekiq/wiki/Deployment
bundle exec sidekiq -d

# Start the database service (and make accessible outside the Docker container)
echo "Starting Rails server..."
if [ -n "$JACOCO" ]; then
  bundle exec rails server -b 0.0.0.0 -p 3000
else
  bundle exec rails server -b 0.0.0.0 -p 3000 2>&1 | tee -a /var/log/dpc-web-$(hostname).log
fi
