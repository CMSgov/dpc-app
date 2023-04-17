# frozen_string_literal: true

Sidekiq.configure_server do |config|
  # Logs are consumed by Splunk via STDOUT. To easily differentiate between rails logs
  # and sidekiq logs, we append `process -> sidekiq-server` to every sidekiq log message
  # NOTE: This can be removed if/when sidekiq server is moved to its own container
  class DPCJSON < Sidekiq::Logger::Formatters::JSON
    def call(severity, time, program_name, message)
      log_msg = JSON.parse(super)
      log_msg['process'] = 'sidekiq-server'
      Sidekiq.dump_json(log_msg) << "\n"
    end
  end

  config.logger.formatter = DPCJSON.new

  config.redis = { url: "#{ENV.fetch('REDIS_URL', 'redis://localhost')}:6379/1" }
end

Sidekiq.configure_client do |config|
  config.redis = { url: "#{ENV.fetch('REDIS_URL', 'redis://localhost')}:6379/1" }
end
