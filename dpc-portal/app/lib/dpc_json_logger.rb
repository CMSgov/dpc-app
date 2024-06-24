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

  def fatal(*msg, &)
    value = as_hash(msg[0], msg[1], &)
    super(value, &nil)
  end

  def unknown(*msg, &)
    value = as_hash(msg[0], msg[1], &)
    super(value, &nil)
  end

  private

  def as_hash(msg, attribs = {})
    msg = yield if block_given?
    raise ArgumentError, 'message must be a string' unless msg.is_a?(String)

    { message: msg }.merge(attribs || {}).merge(CurrentAttributes.to_log_hash)
  end
end
