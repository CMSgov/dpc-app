# frozen_string_literal: true

Rails.application.configure do
  config.lograge.enabled = true
  config.lograge.logger = ActiveSupport::Logger.new(STDOUT)
  config.lograge.formatter = Lograge::Formatters::Json.new

  config.lograge.custom_options = lambda do |event|
    info = { 
      ddsource: 'ruby',
      environment: ENV['ENV'] || :development,
      level: ENV['LOG_LEVEL'] || :info,
      request_id: event.payload[:request].request_id,
    }

    exception = event.payload[:exception_object]

    if exception
      info[:exception_message] = exception.message
      info[:exception_class] = exception.class
      info[:exception_backtrace] = exception.backtrace
    end

    current_user = event.payload[:current_user]
    info[:current_user] = current_user if current_user

    organization = event.payload[:organization]
    info[:organization] = organization if organization
    info
  end
end
