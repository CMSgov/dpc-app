# frozen_string_literal: true

require 'json'

# Custom JSON logger for DPC.
#
# Usage:
#  logger.info("Doing a thing", attempt: 2)
#
class DpcJsonLogger < ActiveSupport::Logger
  def self.formatter
    proc do |severity, time, progname, msg|
      metadata = { level: severity, time:, application: progname, environment: ENV['ENV'] || :development }
      msg = { message: msg } if msg.is_a?(String)
      "#{metadata.merge(msg).compact.to_json}\n"
    end
  end

  def debug(*msg, &)
    value = as_hash(msg[0], msg[1], &)
    super(value, &nil)
  end

  def info(*msg, &)
    value = as_hash(msg[0], msg[1], &)
    super(value, &nil)
  end

  def warn(*msg, &)
    value = as_hash(msg[0], msg[1], &)
    super(value, &nil)
  end

  def error(*msg, &)
    value = as_hash(msg[0], msg[1], &)
    super(value, &nil)
  end

  private

  def as_hash(msg, attribs = {})
    msg = yield if block_given?
    raise ArgumentError.new('message must be a string') unless msg.is_a?(String)

    { message: msg }.merge(attribs || {}).merge(
      request_id: CurrentAttributes.request_id,
      request_user_agent: CurrentAttributes.request_user_agent,
      request_ip: CurrentAttributes.request_ip,
      method: CurrentAttributes.method,
      path: CurrentAttributes.path,
      current_user: CurrentAttributes.current_user,
      organization: CurrentAttributes.organization
    )
  end
end

Rails.application.configure do
    Rails.logger = DpcJsonLogger.new($stdout)
    config.logger = Rails.logger
    config.logger.formatter = DpcJsonLogger.formatter
    config.log_formatter = DpcJsonLogger.formatter
end