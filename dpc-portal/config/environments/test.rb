# frozen_string_literal: true

require 'active_support/core_ext/integer/time'

# The test environment is used exclusively to run your application's
# test suite. You never need to work with it otherwise. Remember that
# your test database is "scratch space" for the test suite and is wiped
# and recreated between test runs. Don't rely on the data there!

Rails.application.configure do
  config.colorize_logging = false
  # Settings specified here will take precedence over those in config/application.rb.

  # While tests run files are not watched, reloading is not necessary.
  config.enable_reloading = false

  # Eager loading loads your entire application. When running a single test locally,
  # this is usually not necessary, and can slow down your test suite. However, it's
  # recommended that you enable it in continuous integration systems to ensure eager
  # loading is working properly before deploying your code.
  config.eager_load = ENV['CI'].present?

  # Configure public file server for tests with cache-control for performance.
  config.public_file_server.enabled = true
  config.public_file_server.headers = { "cache-control" => "public, max-age=3600" }

  # Show full error reports.
  config.consider_all_requests_local = true
  config.cache_store = :null_store

  # Render exception templates for rescuable exceptions and raise for other exceptions.
  # config.action_dispatch.show_exceptions = :rescuable
  config.action_dispatch.show_exceptions = :none

  # Disable request forgery protection in test environment.
  config.action_controller.allow_forgery_protection = false

  # Store uploaded files on the local file system in a temporary directory.
  config.active_storage.service = :test

  # Tell Action Mailer not to deliver emails to the real world.
  # The :test delivery method accumulates sent emails in the
  # ActionMailer::Base.deliveries array.
  config.action_mailer.delivery_method = :test

  config.action_mailer.perform_caching = false

  # Set host to be used by links generated in mailer templates.
  config.action_mailer.default_url_options = { host: 'localhost', port: 3100 }
  config.action_mailer.asset_host = "http://localhost:3100"
  # Need to have this set up for capybara accessibility tests
  config.action_controller.asset_host = "file://#{::Rails.root}/public" if ENV['ACCESSIBILITY'] == 'true'

  # Print deprecation notices to the stderr.
  config.active_support.deprecation = :stderr

  # Tell ActiveJob to not send real requests
  config.active_job.queue_adapter = :test

  # Raises error for missing translations.
  # config.i18n.raise_on_missing_translations = true

  # Annotate rendered view with file names.
  # config.action_view.annotate_rendered_view_with_filenames = true

  # Raise error when a before_action's only/except options reference missing actions.
  # config.action_controller.raise_on_missing_callback_actions = true
end
ENV['CPI_API_GW_BASE_URL'] = 'https://val.cpiapi.cms.gov/'
ENV['CMS_IDM_OAUTH_URL'] = 'https://impl.idp.idm.cms.gov/'
ENV['IDP_LOGIN_DOT_GOV_HOST'] = 'idp.int.identitysandbox.gov'
ENV['IDP_ID_ME_HOST'] = 'api.idmelabs.com'
ENV['CLEAR_IDP_HOST'] = 'verified.clearme.com'
