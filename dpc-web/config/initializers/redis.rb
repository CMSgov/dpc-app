# frozen_string_literal: true

# TODO: Sidekiq uses a depricated `exists` call for Redis. This config
# option suppresses the warning. It will need to be removed once sidekiq
# updates and conforms to the new `exists` structure.
Redis.exists_returns_integer = false
