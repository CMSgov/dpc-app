Rails.application.configure do
  config.lograge.ignore_actions = ["HealthCheck::HealthCheckController#index"]

  config.lograge.custom_options = lambda do |event|
    { ddsource: 'ruby',
      params: event.payload[:params].reject { |k| %w(controller action).include? k },
      environment: ENV['ENV'] || :development,
      exception: event.payload[:exception],
      level: ENV['LOG_LEVEL'] || :info
    }
  end
end

