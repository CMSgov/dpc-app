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

  # Disable serving static files from the `/public` folder by default since
  # Apache or NGINX already handles this.
  config.public_file_server.enabled = true

  # Compress JavaScripts and CSS.
  config.assets.js_compressor = Uglifier.new(:harmony => true)
  # config.assets.css_compressor = :sass

  # Do not fallback to assets pipeline if a precompiled asset is missed.
  config.assets.compile = true

  # `config.assets.precompile` and `config.assets.version` have moved to config/initializers/assets.rb

  # Enable serving of images, stylesheets, and JavaScripts from an asset server.
  # config.asset_host = "http://assets.example.com"

  # Store uploaded files on the local file system (see config/storage.yml for options).
  config.active_storage.service = :local

  # Assume all access to the app is happening through a SSL-terminating reverse proxy.
  # config.assume_ssl = true

  # Force all access to the app over SSL, use Strict-Transport-Security, and use secure cookies.
  config.force_ssl = true
  # Skip http-to-https redirect for the default health check endpoint.
  config.ssl_options = { redirect: { exclude: -> request { request.path =~ /health_check/ } } }

  # Lograge config
  config.lograge.enabled = true

  # This specifies to log in JSON format
  config.lograge.formatter = Lograge::Formatters::Json.new

  ## Disables log coloration
  config.colorize_logging = false

  # Log to a dedicated file
  config.lograge.logger = ActiveSupport::Logger.new(STDOUT)

  # This is useful if you want to log query parameters
  config.lograge.custom_options = lambda do |event|
    { :ddsource => 'ruby',
      :params => event.payload[:params].reject { |k| %w(controller action).include? k }
    }
  end

  # Log to STDOUT with the current request id as a default log tag.
  config.log_tags = [ :request_id ]

  # Change to "debug" to log everything (including potentially personally-identifiable information!)
  config.log_level = ENV["LOG_LEVEL"] || :info

  config.action_mailer.perform_caching = false
  config.action_mailer.default_url_options = { host: ENV['HOST_NAME'], port: ENV['PORT'] }
  config.action_mailer.asset_host = "https://#{ENV['HOST_NAME']}:#{ENV['PORT']}"

  config.action_mailer.delivery_method = :smtp
  config.action_mailer.smtp_settings = {
    address:              ENV['SMTP_ADDRESS'],
    port:                 ENV['SMTP_PORT'],
    domain:               ENV['SMTP_DOMAIN'],
    user_name:            ENV['SMTP_USER_NAME'],
    password:             ENV['SMTP_PASSWORD'],
    authentication:       ENV['SMTP_AUTH'],
    openssl_verify_mode:  'peer',
    tls: true,
    ca_file: '/etc/ssl/certs/ca-certificates.crt'
  }

  # Ignore bad email addresses and do not raise email delivery errors.
  # Set this to true and configure the email server for immediate delivery to raise delivery errors.
  # config.action_mailer.raise_delivery_errors = false

  # Enable locale fallbacks for I18n (makes lookups for any locale fall back to
  # the I18n.default_locale when a translation cannot be found).
  config.i18n.fallbacks = [I18n.default_locale]

  # Send deprecation notices to registered listeners.
  config.active_support.deprecation = :notify

  # Do not dump schema after migrations.
  config.active_record.dump_schema_after_migration = false

  # Only use :id for inspections in production.
  config.active_record.attributes_for_inspect = [ :id ]
end
