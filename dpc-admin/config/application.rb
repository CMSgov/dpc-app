require_relative 'boot'

require 'rails/all'

# Require the gems listed in Gemfile, including any gems
# you've limited to :test, :development, or :production.
Bundler.require(*Rails.groups)

module Admin
  class Application < Rails::Application
    # Dump and read as sql
    config.active_record.schema_format = :sql

    # Initialize configuration defaults for originally generated Rails version.
    config.load_defaults 7.1

    # Add fonts to asset pipeline
    config.assets.prefix = '/admin/assets'
    config.assets.paths << Rails.root.join("app", "assets", "fonts")

    # Settings in config/environments/* take precedence over those specified here.
    # Application configuration can go into files in config/initializers
    # -- all .rb files in that directory are automatically loaded after loading
    # the framework and any gems in your application.

    config.active_job.queue_adapter = :sidekiq

    # Sending mail with`DeliveryJob` has been deprecated. Work has been moved to `MailDeliveryJob`
    config.action_mailer.delivery_job = 'ActionMailer::MailDeliveryJob'

    # Ensure mailer jobs get sent to a specialized admin queue. Our web applications share
    # a single Redis instance and process jobs based on their queue name.
    config.action_mailer.deliver_later_queue_name = "admin"

    config.to_prepare { Devise::Mailer.layout 'mailer' }

    # Mail throttling
    # Default limit to 5 emails before hard stop
    config.x.mail_throttle.limit = ENV.fetch('MAIL_THROTTLE_LIMIT', 5)
    config.x.mail_throttle.expiration = 300 # In seconds
  end
end
