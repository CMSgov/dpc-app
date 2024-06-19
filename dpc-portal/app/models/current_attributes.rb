# frozen_string_literal: true

# ActiveSupport::CurrentAttributes gives us a thread-safe way
# to store request-scope attributes such as request_id.
class CurrentAttributes < ActiveSupport::CurrentAttributes
  attribute :request_id, :request_user_agent, :request_ip, :forwarded_for
  attribute :method, :path
  attribute :current_user
  attribute :organization
end
