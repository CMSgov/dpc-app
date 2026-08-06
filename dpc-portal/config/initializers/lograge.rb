# frozen_string_literal: true

Rails.application.configure do
  config.lograge.enabled = true
  config.lograge.logger = ActiveSupport::Logger.new(STDOUT) unless ENV['DISABLE_JSON_LOGGER'] == 'true'
  config.lograge.formatter = Lograge::Formatters::Json.new

  config.lograge.ignore_actions = ["HealthCheck::HealthCheckController#index"]

  config.lograge.custom_options = lambda do |event|
    info = { 
      ddsource: 'ruby',
      environment: ENV['ENV'] || :development,
      level: ENV['LOG_LEVEL'] || :info,
      time: Time.now
    }.merge(CurrentAttributes.to_log_hash)

    exception = event.payload[:exception_object]

    if exception
      info[:exception_class] = exception.class
      info[:exception_reference] = SecureRandom.uuid
    end
    
    info
  end
end
