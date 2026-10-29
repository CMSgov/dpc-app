require "active_support/core_ext/integer/time"

# These settings are applied to deployed environments, where RAILS_ENV=production.
Rails.application.configure do
  # Settings specified here will take precedence over those in config/application.rb.

  # Code is not reloaded between requests.
  config.enable_reloading = false

  # Eager load code on boot for better performance and memory savings (ignored by Rake tasks).
  config.eager_load = true

  # Full error reports are disabled.
  config.consider_all_requests_local = false

  # Turn on fragment caching in view templates.
  config.action_controller.perform_caching = true

  # Cache assets for far-future expiry since they are all digest stamped.
  # config.public_file_server.headers = { "cache-control" => "public, max-age=#{1.year.to_i}" }

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

  # Store uploaded files on the local file system (see config/storage.yml for options).
  config.active_storage.service = :local

  # Assume all access to the app is happening through a SSL-terminating reverse proxy.
  # config.assume_ssl = true

  # Force all access to the app over SSL, use Strict-Transport-Security, and use secure cookies.
  config.force_ssl = true
  # The load balancer health check comes in over http, so we need to exclude it.
  config.ssl_options = {
    redirect: {
      exclude: ->(request) { request.path == "/health_check" }
    }
  }

  ## Disables log coloration
  config.colorize_logging = false

  # Log to STDOUT with the current request id as a default log tag.
  # config.log_tags = [ :request_id ]
  # config.logger   = ActiveSupport::TaggedLogging.logger(STDOUT)

  # Change to "debug" to log everything (including potentially personally-identifiable information!)
  config.log_level = ENV.fetch("RAILS_LOG_LEVEL", "info")

  # Prevent health checks from clogging up the logs.
  config.silence_healthcheck_path = "/health_check"

  # Don't log any deprecations.
  config.active_support.report_deprecations = false

  # Replace the default in-process memory cache store with a durable alternative.
  # config.cache_store = :mem_cache_store

  # Replace the default in-process and non-durable queuing backend for Active Job.
  # config.active_job.queue_adapter = :resque

  config.action_mailer.perform_caching = false

  config.action_mailer.default_url_options = { host: ENV['HOST_NAME'], port: ENV['PORT'] }
  config.action_mailer.asset_host = "https://#{ENV['HOST_NAME']}:#{ENV['PORT']}"

  config.action_mailer.delivery_method = :smtp
  config.action_mailer.smtp_settings = {
    address:         ENV['SMTP_ADDRESS'],
    port:            ENV['SMTP_PORT'],
    domain:          ENV['SMTP_DOMAIN'],
    openssl_verify_mode:  'peer',
    tls: true,
    ca_file: '/etc/ssl/certs/ca-certificates.crt'
  }

  # Ignore bad email addresses and do not raise email delivery errors.
  # Set this to true and configure the email server for immediate delivery to raise delivery errors.
  # config.action_mailer.raise_delivery_errors = false

  # Enable locale fallbacks for I18n (makes lookups for any locale fall back to
  # the I18n.default_locale when a translation cannot be found).
  config.i18n.fallbacks = true

  # Do not dump schema after migrations.
  config.active_record.dump_schema_after_migration = false

  # Only use :id for inspections in production.
  config.active_record.attributes_for_inspect = [ :id ]

  # Enable DNS rebinding protection and other `Host` header attacks.
  # config.hosts = [
  #   "example.com",     # Allow requests from example.com
  #   /.*\.example\.com/ # Allow requests from subdomains like `www.example.com`
  # ]
  #
  # Skip DNS rebinding protection for the default health check endpoint.
  # config.host_authorization = { exclude: ->(request) { request.path == "/up" } }
end
# Do not log values for hashie (used by omniauth and warns in calls to IDM)
# See https://github.com/omniauth/omniauth/issues/872#issuecomment-276501012
# Only needs prod, as we stub IDM locally
Hashie.logger = Logger.new(nil)
