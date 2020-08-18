# frozen_string_literal: true

require 'redis-namespace'

module RedisStore
  class MailThrottleStore
    NAMESPACE = 'mail_throttle'
    LIMIT = 10 # Limit to 10 emails before hard stop
    EXPIRATION = 300 # In seconds

    def initialize
      @redis = Redis::Namespace.new(NAMESPACE, redis: Redis.current)
    end

    def can_email?(key)
      return false if get_value(key) >= LIMIT
      increment(key)
    end

    private

    def increment(key)
      @redis.incr(key)
      @redis.expire(key, EXPIRATION)
    end

    def get_value(key)
      @redis.get(key).to_i
    end
  end
end
