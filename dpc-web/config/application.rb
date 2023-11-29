require_relative 'boot'

# Enable stout syncing for Docker
$stdout.sync = true

require "rails"
require "luhnacy"
# Pick the frameworks you want:
require "active_model/railtie"
require "active_job/railtie"
require "active_record/railtie"
require "active_storage/engine"
require "action_controller/railtie"
require "action_mailer/railtie"
require "action_view/railtie"
require "sprockets/railtie"
# require "rails/test_unit/railtie"
require "./lib/dpc_middleware/ig_fix"
require 'macaroons'

# Require the gems listed in Gemfile, including any gems
# you've limited to :test, :development, or :production.
Bundler.require(*Rails.groups)

module DpcWebsite
  class Application < Rails::Application

    #Dump and read as sql
    config.active_record.schema_format = :sql

    # Check for STATIC_SITE_URL environment variable
    ENV['STATIC_SITE_URL'].present? ? ENV['STATIC_SITE_URL'] : ENV['STATIC_SITE_URL'] = 'https://dpc.cms.gov'
    config.autoload_paths << Rails.root.join('lib')
    config.autoload_paths << Rails.root.join('lib/luhnacy_lib')

    # Initialize configuration defaults for originally generated Rails version.
    config.load_defaults 5.2

    # Add fonts to asset pipeline
    config.assets.paths << Rails.root.join("app", "assets", "fonts")

    # Settings in config/environments/* take precedence over those specified here.
    # Application configuration can go into files in config/initializers
    # -- all .rb files in that directory are automatically loaded after loading
    # the framework and any gems in your application.

    # Don't generate system test files.
    config.generators.system_tests = nil

    # field_with_errors support, avoid that nasty line break on errors
    config.action_view.field_error_proc = Proc.new { |html_tag, instance|
      html_tag
    }

    # Add middleware to fix issue with /ig links breaking
    config.middleware.insert_before ActionDispatch::Static, DpcMiddleware::IgFix

    config.active_job.queue_adapter = :sidekiq

    # Sending mail with`DeliveryJob` has been deprecated. Work has been moved to `MailDeliveryJob`
    config.action_mailer.delivery_job = "ActionMailer::MailDeliveryJob"
    
    # Ensure mailer jobs get sent to a specialized web queue. Our web applications share
    # a single Redis instance and process jobs based on their queue name.
    config.action_mailer.deliver_later_queue_name = "admin"
    
    config.to_prepare { Devise::Mailer.layout "mailer" }

    # Mail throttling
    # Default limit to 5 emails before hard stop
    config.x.mail_throttle.limit = ENV.fetch('MAIL_THROTTLE_LIMIT', 5)
    config.x.mail_throttle.expiration = 300 # In seconds
  end
end
