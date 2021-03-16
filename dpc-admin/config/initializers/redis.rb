# frozen_string_literal: true

# environment specific redis host
REDIS_URL = "#{ENV.fetch('REDIS_URL', 'redis://localhost')}:6379/1".freeze

Redis.current = Redis.new(url: REDIS_URL)
