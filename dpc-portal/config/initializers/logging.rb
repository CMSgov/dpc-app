# frozen_string_literal: true

require './app/lib/dpc_json_logger'

Rails.application.configure do
  unless ENV['DISABLE_JSON_LOGGER'] == 'true'
    Rails.logger = DpcJsonLogger.new($stdout)
    config.logger = Rails.logger
    config.logger.formatter = DpcJsonLogger.formatter
    config.log_formatter = DpcJsonLogger.formatter
  end
end
