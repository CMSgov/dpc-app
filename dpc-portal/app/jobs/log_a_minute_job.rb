class LogAMinuteJob < ApplicationJob
  queue_as :portal

  def perform
    logger.info("LOGGING AT #{Time.now}")
  end
end
