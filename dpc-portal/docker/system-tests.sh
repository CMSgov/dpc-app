#!/usr/bin/env sh

apk update
apk add --no-cache icu-data-full
apk add --no-cache firefox
SYSTEM_TEST=true bundle exec rspec --exclude-pattern "spec/system/accessibility_spec.rb" --tag type:system
