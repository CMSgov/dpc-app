Rails.application.configure do
  # Settings specified here will take precedence over those in config/application.rb.

  # In the development environment your application's code is reloaded on
  # every request. This slows down response time but is perfect for development
  # since you don't have to restart the web server when you make code changes.
  config.cache_classes = false

  # Do not eager load code on boot.
  config.eager_load = false

  # Show full error reports.
  config.consider_all_requests_local = true

  # don't cache controller
  config.action_controller.perform_caching = false

  # use redis cache for session
  config.cache_store = :redis_cache_store, { url: "#{ENV.fetch('REDIS_URL', 'redis://localhost')}:6379/1" }

  # Store uploaded files on the local file system (see config/storage.yml for options)
  config.active_storage.service = :local

  # Don't care if the mailer can't send.
  config.action_mailer.raise_delivery_errors = false

  config.action_mailer.perform_caching = false

  config.action_mailer.perform_deliveries = true

  # By default we use letter_opener to render the email in a browser
  # rather than send them:
  config.action_mailer.delivery_method = :letter_opener_web

  # For sending emails locally:
  # config.action_mailer.delivery_method = :smtp
  # config.action_mailer.smtp_settings = {
  #   address:              ENV['SMTP_ADDRESS'],
  #   port:                 ENV['SMTP_PORT'],
  #   domain:               ENV['SMTP_DOMAIN'],
  #   user_name:            ENV['SMTP_USER_NAME'],
  #   password:             ENV['SMTP_PASSWORD'],
  #   authentication:       ENV['SMTP_AUTH'],
  #   openssl_verify_mode:  ENV['SMTP_SSL_VERIFY'],
  #   enable_starttls_auto: true
  # }

  # Print deprecation notices to the Rails logger.
  config.active_support.deprecation = :log

  # Raise an error on page load if there are pending migrations.
  config.active_record.migration_error = :page_load

  # Highlight code that triggered database queries in logs.
  config.active_record.verbose_query_logs = true

  # Debug mode disables concatenation and preprocessing of assets.
  # This option may cause significant delays in view rendering with a large
  # number of complex assets.
  config.assets.debug = true

  # Suppress logger output for asset requests.
  config.assets.quiet = true

  # Raises error for missing translations
  # config.action_view.raise_on_missing_translations = true

  # Use an evented file watcher to asynchronously detect changes in source code,
  # routes, locales, etc. This feature depends on the listen gem.
  config.file_watcher = ActiveSupport::EventedFileUpdateChecker

  # Inline source maps
  config.sass.inline_source_maps = true

  # Devise requires mailer
  config.action_mailer.default_url_options = { host: 'localhost', port: 3900 }
  config.action_mailer.asset_host = "http://localhost:3900"
end
