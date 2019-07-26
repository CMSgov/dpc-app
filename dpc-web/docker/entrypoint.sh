#!/usr/bin/env sh

set -e

# Clean the existing pid if the server was previously started (using docker-compose for example)
if [ -f tmp/pids/server.pid ]; then
  rm tmp/pids/server.pid
fi

# Compile the web assets
echo "Compiling web assets..."
rails assets:precompile

# Run the database migrations
echo "Migrating the database..."
rails db:migrate

# Seed the database
# This step is not needed, as there is no database seed data yet

# Start the database service (and make accessible outside the Docker container)
echo "Starting Rails server..."
rails server -b 0.0.0.0