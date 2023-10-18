# frozen_string_literal: true

require 'spec_helper'
ENV['RAILS_ENV'] ||= 'test'

require File.expand_path('../config/environment', __dir__)

# Prevent database truncation if the environment is production
abort('The Rails environment is running in production mode!') if Rails.env.production?

require 'rspec/rails'

# Add additional requires below this line. Rails is not loaded until this point!
require 'capybara/rails'
require 'capybara/rspec'
# require 'support/chromedriver'
require 'support/dpc_client_support'
require 'webmock/rspec'

# Dir[Rails.root.join('spec', 'support', '**', '*.rb')].each { |f| require f }

# Checks for pending migrations and applies them before tests are run.
# If you are not using ActiveRecord, you can remove these lines.
begin
  ActiveRecord::Migration.maintain_test_schema!
rescue ActiveRecord::PendingMigrationError => e
  puts e.to_s.strip
  exit 1
end

# require 'sidekiq/testing'
# Sidekiq::Testing.fake!

RSpec.configure do |config|
  config.include FactoryBot::Syntax::Methods
  config.include Rails.application.routes.url_helpers

  # Devise test helpers
  config.include Devise::Test::ControllerHelpers, type: :controller
  config.include Devise::Test::ControllerHelpers, type: :view
  config.include Devise::Test::IntegrationHelpers, type: :feature
  config.include Warden::Test::Helpers

  Warden.test_mode!

  config.use_transactional_fixtures = false
  config.before(:suite) { DatabaseCleaner.clean_with(:truncation) }
  config.before(:each) { DatabaseCleaner.strategy = :transaction }
  config.before(:each, js: true) { DatabaseCleaner.strategy = :truncation }

  config.before(:each) { DatabaseCleaner.start }

  config.after(:each, type: :feature) { Warden.test_reset! }

  # It's recommended to use append_after to ensure DatabaseCleaner.clean
  # runs after the after-test cleanup capybara/rspec installs. Particularly
  # on js tests.
  config.append_after(:each) { DatabaseCleaner.clean }

  config.infer_spec_type_from_file_location!
  config.filter_rails_from_backtrace!

  config.around(:each, :perform_enqueued) do |example|
    Delayed::Worker.delay_jobs = false

    example.run

    Delayed::Worker.delay_jobs = true
  end
end
