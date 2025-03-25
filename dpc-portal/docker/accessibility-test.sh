#!/usr/bin/env sh

apk update
apk add --no-cache icu-data-full
apk add --no-cache firefox
ACCESSIBILITY=true bundle exec rspec --tag type:system --format documentation
