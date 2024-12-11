# frozen_string_literal: true

require 'redis-namespace'

module RedisStore
  class MailThrottleStore
    # Redis interface for rate limiting user mail

    NAMESPACE = 'mail_throttle'

    def initialize
      # Retrieve redis instance with a provided namespace
      @limit = Rails.configuration.x.mail_throttle.limit
      @expiration = Rails.configuration.x.mail_throttle.expiration
      redis_url = "#{ENV.fetch('REDIS_URL', 'redis://localhost')}:6379/1"
      @redis = Redis::Namespace.new(NAMESPACE, redis: Redis.new(url: redis_url))
    end

    def can_email?(key)
      return false if get_value(key) >= @limit

      increment(key)
    end

    private

    def increment(key)
      @redis.incr(key)
      @redis.expire(key, @expiration)
    end

    def get_value(key)
      @redis.get(key).to_i
    end
  end
end
