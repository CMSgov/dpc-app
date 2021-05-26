require_relative "boot"

# Enable stout syncing for Docker
$stdout.sync = true

require "rails/all"
require "active_model/railtie"
require "active_job/railtie"
require "active_record/railtie"
require "active_storage/engine"
require "action_controller/railtie"
require "action_mailer/railtie"
require "action_view/railtie"
require "action_cable/engine"

# Require the gems listed in Gemfile, including any gems
# you've limited to :test, :development, or :production.
Bundler.require(*Rails.groups)

module DpcImpl
  class Application < Rails::Application
    # ump and read as sql
    config.active_record.schema_format = :sql

    # Check for STATIC_SITE_URL environment variable
    ENV['STATIC_SITE_URL'].present? ? ENV['STATIC_SITE_URL'] : ENV['STATIC_SITE_URL'] = 'https://dpc.cms.gov'

    # Initialize configuration defaults for originally generated Rails version.
    config.load_defaults 6.1

    # Add fonts to asset pipeline
    config.assets.prefix = '/impl/assets'
    config.assets.paths << Rails.root.join("app", "assets", "fonts")

    # Don't generate system test files.
    config.generators.system_tests = nil

    # field_with_errors support, avoid that nasty line break on errors
    config.action_view.field_error_proc = Proc.new { |html_tag, instance|
      html_tag
    }

    config.active_job.queue_adapter = :sidekiq

    # Sending mail with`DeliveryJob` has been deprecated. Work has been moved to `MailDeliveryJob`
    config.action_mailer.delivery_job = 'ActionMailer::MailDeliveryJob'

    config.to_prepare { Devise::Mailer.layout 'mailer' }

    # Mail throttling
    # Default limit to 5 emails before hard stop
    config.x.mail_throttle.limit = ENV.fetch('MAIL_THROTTLE_LIMIT', 5)
    config.x.mail_throttle.expiration = 300 # In seconds
  end
end
