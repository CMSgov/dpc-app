# frozen_string_literal: true

Rails.application.configure do
  config.lograge.enabled = true
  config.lograge.logger = ActiveSupport::Logger.new(STDOUT) unless ENV['DISABLE_JSON_LOGGER'] == 'true'
  config.lograge.formatter = Lograge::Formatters::Json.new

  config.lograge.custom_options = lambda do |event|
    info = { 
      ddsource: 'ruby',
      environment: ENV['ENV'] || :development,
      level: ENV['LOG_LEVEL'] || :info,
      time: Time.now
    }.merge(CurrentAttributes.to_log_hash)

    exception = event.payload[:exception_object]

    if exception
      info[:exception_message] = exception.message
      info[:exception_class] = exception.class
      info[:exception_backtrace] = Rails.backtrace_cleaner.clean(exception.backtrace)
    end
    
    info
  end
end
