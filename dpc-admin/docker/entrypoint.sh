#!/usr/bin/env sh

set -e

# Clean the existing pid if the server was previously started (using docker-compose for example)
if [ -f tmp/pids/server.pid ]; then
  rm tmp/pids/server.pid
fi

if [ "$1" == "admin" ]; then
  # Autogenerate fresh golden macaroons in local development
  if [[ "$RAILS_ENV" != "production" ]] && [[ -z "$GOLDEN_MACAROON" ]]; then
    echo "No golden macaroon found. Attempting to generate a new one..."
    export GOLDEN_MACAROON=$(curl -X POST -w '\n' ${API_ADMIN_URL}/tasks/generate-token || echo '')

    if [ -n "$GOLDEN_MACAROON" ]; then
      echo "Successfully generated new golden macaroon."
    else
      echo "Could not generate a valid golden macaroon. Check that the API service is running."
      echo "Starting Portal without a golden macaroon; certain functionality may not work correctly."
    fi
  fi

  echo "Starting Rails server..."
  bundle exec rails server -b 0.0.0.0 -p 3000
fi

if [ "$1" == "sidekiq" ]; then
  # Start Sidekiq job processing
  bundle exec sidekiq -q admin
fi
