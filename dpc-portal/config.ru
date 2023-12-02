# frozen_string_literal: true

# This file is used by Rack-based servers to start the application.

require_relative 'config/environment'

Rails.application.routes.default_url_options[:script_name] = ENV['RAILS_RELATIVE_URL_ROOT']

# Ensure the relative url root is read and respected by puma.
# This is needed to ensure that all routes are sub-pathed under /portal
# since the web applications share the same base URL (<env>.dpc.cms.gov).
map Rails.application.config.relative_url_root || '/' do
    run Rails.application
    Rails.application.load_server
end
