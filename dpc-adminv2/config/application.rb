require_relative "boot"

require "rails/all"

# Require the gems listed in Gemfile, including any gems
# you've limited to :test, :development, or :production.
Bundler.require(*Rails.groups)

module DpcAdminv2
  class Application < Rails::Application
    # Check for STATIC_SITE_URL environment variable
    ENV['STATIC_SITE_URL'].present? ? ENV['STATIC_SITE_URL'] : ENV['STATIC_SITE_URL'] = 'https://dpc.cms.gov'

    # Initialize configuration defaults for originally generated Rails version.
    config.load_defaults 6.1

    # Add fonts to asset pipeline
    config.assets.prefix = '/adminv2/assets'
    config.assets.paths << Rails.root.join("app", "assets", "fonts")
    # Configuration for the application, engines, and railties goes here.
    #
    # These settings can be overridden in specific environments using the files
    # in config/environments, which are processed later.
    #
    # config.time_zone = "Central Time (US & Canada)"
    # config.eager_load_paths << Rails.root.join("extras")

    config.active_job.queue_adapter = :sidekiq
  end
end
