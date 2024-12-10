# frozen_string_literal: true

# Logs async for procedures run via ssh
# Actions activated by rake tasks on AWS servers are not logging to CloudWatch, so we need
# to have sidekiq do the logging, since its logs seem to go through
class AsyncLoggerJob < ApplicationJob
  queue_as :default
  LOGGER_LEVELS = %i[debug info warn error fatal].freeze

  def perform(level, args)
    if LOGGER_LEVELS.include?(level)
      Rails.logger.send(level, args)
    else
      Rails.logger.unknown(args)
    end
  end
end
