require_relative "boot"

require "rails/all"

# Require the gems listed in Gemfile, including any gems
# you've limited to :test, :development, or :production.
Bundler.require(*Rails.groups)

module DpcImpl
  class Application < Rails::Application
    # Initialize configuration defaults for originally generated Rails version.
    config.load_defaults 6.1

    # Add fonts to asset pipeline
    config.assets.paths << Rails.root.join("app", "assets", "fonts")

    # field_with_errors support, avoid that nasty line break on errors
    config.action_view.field_error_proc = Proc.new { |html_tag, instance|
      html_tag
    }

    config.to_prepare { Devise::Mailer.layout "mailer" }
  end
end
