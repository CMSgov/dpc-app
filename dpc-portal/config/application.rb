# frozen_string_literal: true

require_relative 'boot'

require 'rails/all'

# Require the gems listed in Gemfile, including any gems
# you've limited to :test, :development, or :production.
Bundler.require(*Rails.groups)

module DpcPortal
  class Application < Rails::Application
    # Initialize configuration defaults for originally generated Rails version.
    config.load_defaults 7.0

    # Set the relative_url_root at runtime, which will be used in various places
    # to ensure that we are serving everything under the portal scope.
    config.relative_url_root = '/portal'

    # Configuration for the application, engines, and railties goes here.
    #
    # These settings can be overridden in specific environments using the files
    # in config/environments, which are processed later.
    #
    # config.time_zone = "Central Time (US & Canada)"
    # config.eager_load_paths << Rails.root.join("extras")

    config.active_job.queue_adapter = :sidekiq
    
    # Ensure mailer jobs get sent to a specialized admin queue. Our web applications share
    # a single Redis instance and process jobs based on their queue name.
    config.action_mailer.deliver_later_queue_name = "portal"

    # Look up previews directly in the path and set default layout
    config.view_component.preview_paths << Rails.root.join("app", "components")
    config.view_component.default_preview_layout = "component_preview"
  end
end
