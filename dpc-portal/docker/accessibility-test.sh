#!/usr/bin/env sh

apk add --no-cache firefox
ACCESSIBILITY=true bundle exec rspec --tag type:system
