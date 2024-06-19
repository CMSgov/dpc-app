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
      request_id: CurrentAttributes.request_id,
      request_user_agent: CurrentAttributes.request_user_agent,
      request_ip: CurrentAttributes.request_ip,
      method: CurrentAttributes.method,
      path: CurrentAttributes.path,
    }

    exception = event.payload[:exception_object]

    if exception
      info[:exception_message] = exception.message
      info[:exception_class] = exception.class
      info[:exception_backtrace] = Rails.backtrace_cleaner.clean(exception.backtrace)
    end
    
    # Insert optional information added during the request. See the ApplicationController.
    current_user = CurrentAttributes.current_user
    info[:current_user] = current_user if current_user

    organization = CurrentAttributes.organization
    info[:organization] = organization if organization
    info
  end
end
