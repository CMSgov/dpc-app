# frozen_string_literal: true

# ActiveSupport::CurrentAttributes gives us a thread-safe way
# to store request-scope attributes such as request_id.
class CurrentAttributes < ActiveSupport::CurrentAttributes
  attribute :request_id, :request_user_agent, :request_ip, :forwarded_for
  attribute :method, :path
  attribute :current_user
  attribute :organization

  def to_log_hash
    {
      request_id: CurrentAttributes.request_id,
      request_user_agent: CurrentAttributes.request_user_agent,
      request_ip: CurrentAttributes.request_ip,
      method: CurrentAttributes.method,
      path: CurrentAttributes.path,
      current_user: CurrentAttributes.current_user,
      organization: CurrentAttributes.organization
    }
  end
end
