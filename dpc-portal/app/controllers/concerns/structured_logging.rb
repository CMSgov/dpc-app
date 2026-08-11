# frozen_string_literal: true

# Provides a consistent structured logging interface across controllers.
# Automatically merges csp_log_context and timestamps into every log entry.
module StructuredLogging
  extend ActiveSupport::Concern

  def log_event(level, message, action_context:, action_type: nil, **extras)
    payload = build_log_payload(action_context, action_type, extras)
    Rails.logger.public_send(level, [message, payload])
  end

  private

  def build_log_payload(action_context, action_type, extras)
    {
      actionContext: action_context,
      timestamp: Time.now.utc.iso8601,
      **csp_log_context,
      **optional_log_fields(action_type, extras)
    }
  end

  def optional_log_fields(action_type, extras)
    known = {
      actionType: action_type,
      user_identifier: extras[:user_identifier],
      invitation: extras[:invitation],
      csp: extras[:csp_name],
      error: extras[:error]
    }.compact

    remaining = extras.except(:user_identifier, :invitation, :csp_name, :error)

    known.merge(remaining)
  end
end
