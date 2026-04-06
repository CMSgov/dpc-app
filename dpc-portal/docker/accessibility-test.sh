#!/usr/bin/env sh

apk update
apk add --no-cache icu-data-full
apk add --no-cache firefox
SKIP_SIMPLE_COV=true ACCESSIBILITY=true bundle exec rspec spec/system/accessibility_spec.rb --tag type:system --format documentation
