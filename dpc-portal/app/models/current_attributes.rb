# frozen_string_literal: true

# ActiveSupport::CurrentAttributes gives us a thread-safe way
# to store request-scope attributes such as request_id.
class CurrentAttributes < ActiveSupport::CurrentAttributes
  attribute :request_id, :request_user_agent, :request_ip, :forwarded_for
  attribute :method, :path
  attribute :current_user
  attribute :organization

  def set_request_attributes(_req)
    CurrentAttributes.request_id = request.request_id
    CurrentAttributes.request_user_agent = request.user_agent
    CurrentAttributes.request_ip = request.ip
    CurrentAttributes.forwarded_for = request.headers['X-Forwarded-For']
    CurrentAttributes.method = request.method
    CurrentAttributes.path = request.path
  end

  def set_user_attributes(user)
    return unless user

    CurrentAttributes.current_user = {
      id: user.id,
      external_id: user.uid,
      pac_id: user.pac_id
    }
  end

  def set_organization_attributes(org, user)
    return unless org

    CurrentAttributes.organization = {
      id: org.id,
      dpc_api_organization_id: org.dpc_api_organization_id
    }

    return unless user

    begin
      payload[:organization][:is_authorized_official] = user.ao?(org)
      payload[:organization][:is_credential_delegate] = user.cd?(org)
    rescue err
      Rails.logger.warn('Failed to pull user roles for organization')
    end
  end

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
