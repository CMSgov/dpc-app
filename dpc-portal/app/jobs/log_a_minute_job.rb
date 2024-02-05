# frozen_string_literal: true

# Logs when called
class LogAMinuteJob < ApplicationJob
  queue_as :portal

  def perform
    logger.info("LOGGING AT #{Time.now}")
  end
end
