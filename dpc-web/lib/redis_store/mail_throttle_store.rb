# frozen_string_literal: true

module RedisStore
  class MailThrottleStore
    NAMESPACE = 'mail_throttle'
    # Interface for rate limiting user mail

    def initialize
      # Retrieve redis instance with a provided namespace
      @limit = Rails.configuration.x.mail_throttle.limit
      @expiration = Rails.configuration.x.mail_throttle.expiration
    end

    def can_email?(key)
      namespaced_key = "#{NAMESPACE}/#{key}"
      return false if get_value(namespaced_key) >= @limit

      increment(namespaced_key)
      true
    end

    private

    def increment(key)
      Rails.cache.write(key, get_value(key) + 1, expires_in: @expiration.seconds)
    end

    def get_value(key)
      Rails.cache.fetch(key) { 0 }
    end
  end
end
