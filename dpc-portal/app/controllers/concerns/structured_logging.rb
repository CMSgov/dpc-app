# frozen_string_literal: true

# Provides a consistent structured logging interface across controllers.
# Automatically merges csp_log_context and timestamps into every log entry.
module StructuredLogging
  extend ActiveSupport::Concern

  # Only these fields will be included in the final log.
  # Add a field to this list only after confirming it cannot contain PHI/PII.
  ALLOWED_EXTRA_LOG_FIELDS = %i[
    user_identifier
    invitation
    csp
    csp_name
    error
    organization_npi
    verificationReason
  ].freeze

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
    extras = filter_extras(extras)
    csp_value = extras[:csp] || extras[:csp_name]
    known = {
      actionType: action_type,
      user_identifier: extras[:user_identifier],
      invitation: extras[:invitation],
      csp: csp_value,
      error: extras[:error]
    }.compact
    remaining = extras.except(:user_identifier, :invitation, :csp, :csp_name, :error)
    known.merge(remaining)
  end

  # Drop non-allowlisted fields and warn with key name (not value) in logs
  def filter_extras(extras)
    rejected_keys = extras.keys - ALLOWED_EXTRA_LOG_FIELDS
    if rejected_keys.any?
      Rails.logger.warn(['StructuredLogging: dropped non-allowlisted field(s) from log payload',
                         { rejected_fields: rejected_keys }])
    end

    extras.slice(*ALLOWED_EXTRA_LOG_FIELDS)
  end
end
