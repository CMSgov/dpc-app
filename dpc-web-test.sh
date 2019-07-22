#!/bin/bash

if [ -n "$TRAVIS_SECURE_ENV_VARS" ] && [ "$TRAVIS_SECURE_ENV_VARS" = "true" ]; then
   echo "┌──────────────────────────────────────────┐"
   echo "│                                          │"
   echo "│        Running Website Tests...          │"
   echo "│                                          │"
   echo "└──────────────────────────────────────────┘"

   docker-compose down
   docker-compose -f dpc-web/docker-compose.yml build
   docker-compose -f dpc-web/docker-compose.yml run web rails db:migrate db:seed

   echo "┌──────────────────────────────────────────┐"
   echo "│                                          │"
   echo "│        All Website Tests Complete        │"
   echo "│                                          │"
   echo "└──────────────────────────────────────────┘"
else
    echo "┌──────────────────────────────────────────┐"
    echo "│                                          │"
    echo "│          Skipping Website Tests          │"
    echo "│          (No Secure Environment)         │"
    echo "│                                          │"
    echo "└──────────────────────────────────────────┘"
fi