# frozen_string_literal: true

# Turn off DD telemetry when we're running tests.  I tried doing this in /initializers/test.rb, but apparently DD
# gets loaded before that gets run.  If we leave DD running, we'd need to mock all of the calls to the DD agent, and that
# seems like a nightmare.
if ENV['RAILS_ENV'] == 'test' || ENV['RACK_ENV'] == 'test'
  ENV['DD_TRACE_ENABLED'] = 'false'
  ENV['DD_PROFILING_ENABLED'] = 'false'
end

ENV['BUNDLE_GEMFILE'] ||= File.expand_path('../Gemfile', __dir__)

require 'bundler/setup' # Set up gems listed in the Gemfile.
require 'bootsnap/setup' # Speed up boot time by caching expensive operations.
