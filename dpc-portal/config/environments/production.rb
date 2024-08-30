# frozen_string_literal: true

require 'active_support/core_ext/integer/time'

# These settings are applied to deployed environments, where RAILS_ENV=production.
Rails.application.configure do
  # Settings specified here will take precedence over those in config/application.rb.

  # Code is not reloaded between requests.
  config.cache_classes = true

  # Eager load code on boot. This eager loads most of Rails and
  # your application in memory, allowing both threaded web servers
  # and those relying on copy on write to perform better.
  # Rake tasks automatically ignore this option for performance.
  config.eager_load = true

  # Full error reports are disabled and caching is turned on.
  config.consider_all_requests_local       = false
  config.action_controller.perform_caching = true

  # Ensures that a master key has been made available in either ENV["RAILS_MASTER_KEY"]
  # or in config/master.key. This key is used to decrypt credentials (and other encrypted files).
  # config.require_master_key = true

  # TODO: 
  # We should disable serving static files from the `/public` folder and
  # configure a production-ready static file server separate from Rails.
  # This is recommended to improve static file download performance.
  config.public_file_server.enabled = true

  # Compress CSS using a preprocessor.
  # config.assets.css_compressor = :sass

  # Do not fallback to assets pipeline if a precompiled asset is missed.
  config.assets.compile = false

  # Enable serving of images, stylesheets, and JavaScripts from an asset server.
  # config.asset_host = "http://assets.example.com"

  # Specifies the header that your server uses for sending files.
  # config.action_dispatch.x_sendfile_header = "X-Sendfile" # for Apache
  # config.action_dispatch.x_sendfile_header = "X-Accel-Redirect" # for NGINX

  # Store uploaded files on the local file system (see config/storage.yml for options).
  config.active_storage.service = :local

  # Mount Action Cable outside main process or domain.
  # config.action_cable.mount_path = nil
  # config.action_cable.url = "wss://example.com/cable"
  # config.action_cable.allowed_request_origins = [ "http://example.com", /http:\/\/example.*/ ]

  # Force all access to the app over SSL, use Strict-Transport-Security, and use secure cookies.
  # config.force_ssl = true

  ## Disables log coloration
  config.colorize_logging = false

  # Use the lowest log level to ensure availability of diagnostic information
  # when problems arise.
  config.log_level = ENV["LOG_LEVEL"] || :info

  # Use a different cache store in production.
  # config.cache_store = :mem_cache_store

  # Use a real queuing backend for Active Job (and separate queues per environment).
  # config.active_job.queue_adapter     = :resque
  # config.active_job.queue_name_prefix = "dpc_portal_production"

  config.action_mailer.perform_caching = false

  config.action_mailer.default_url_options = { host: ENV['HOST_NAME'], port: ENV['PORT'] }
  config.action_mailer.asset_host = "https://#{ENV['HOST_NAME']}:#{ENV['PORT']}"

  config.action_mailer.delivery_method = :smtp
  config.action_mailer.smtp_settings = {
    address:         ENV['SMTP_ADDRESS'],
    port:            ENV['SMTP_PORT'],
    domain:          ENV['SMTP_DOMAIN'],
    openssl_verify_mode:  'peer',
    tls: true
  }

  # Ignore bad email addresses and do not raise email delivery errors.
  # Set this to true and configure the email server for immediate delivery to raise delivery errors.
  # config.action_mailer.raise_delivery_errors = false

  # Enable locale fallbacks for I18n (makes lookups for any locale fall back to
  # the I18n.default_locale when a translation cannot be found).
  config.i18n.fallbacks = true

  # Don't log any deprecations.
  config.active_support.report_deprecations = false

  # Do not dump schema after migrations.
  config.active_record.dump_schema_after_migration = false
end
# Do not log values for hashie (used by omniauth and warns in calls to IDM)
# See https://github.com/omniauth/omniauth/issues/872#issuecomment-276501012
# Only needs prod, as we stub IDM locally
Hashie.logger = Logger.new(nil)
