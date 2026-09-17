# frozen_string_literal: true

# Handles errors in CSP flow
module CspErrorHandling
  extend ActiveSupport::Concern

  CSP_AUTH_ERROR_MESSAGES = %w[server_error service_unavailable connection_failed internal_server_error timeout].freeze
  CSP_USER_ERROR_MESSAGES = %w[access_denied].freeze
  CSP_USER_FAIL_TO_PROOF = %w[verification_failure].freeze

  VERIFICATION_ALERT = "We weren't able to complete identity verification."

  def csp_auth_error?
    CSP_AUTH_ERROR_MESSAGES.include?(params[:message])
  end

  def csp_user_cancelled?
    CSP_USER_ERROR_MESSAGES.include?(params[:message])
  end

  def csp_user_fail_to_proof?
    CSP_USER_FAIL_TO_PROOF.include?(params[:message])
  end

  def csp_param
    params[:strategy] || csp_session.current
  end

  def handle_fail_to_proof(invitation)
    log_event(:info, 'User failed identity verification',
              action_context: action_context(invitation),
              action_type: LoggingConstants::ActionType::FailedLogin,
              csp: csp_param)
    render(Page::Utility::VerificationFailureComponent.new(invitation, csp_param))
  end

  def handle_csp_auth_error(invitation)
    log_event(:error, 'CSP Authentication error',
              action_context: action_context(invitation),
              action_type: LoggingConstants::ActionType::CspUnavailable,
              error: params[:message],
              csp: csp_param)
    redirect_to signin_destination(invitation), alert: VERIFICATION_ALERT
  end

  def handle_signin_fail(invitation)
    log_event(:error, 'CSP Configuration error',
              action_context: action_context(invitation),
              action_type: LoggingConstants::ActionType::FailedLogin,
              csp: csp_param)
    redirect_to signin_destination(invitation), alert: VERIFICATION_ALERT
  end

  def handle_signin_cancel(invitation)
    log_event(:info, 'User cancelled login',
              action_context: action_context(invitation),
              action_type: LoggingConstants::ActionType::UserCancelledLogin,
              csp: csp_param)
    redirect_to signin_destination(invitation), alert: VERIFICATION_ALERT
  end

  private

  def action_context(invitation)
    invitation.nil? ? LoggingConstants::ActionContext::Authentication : LoggingConstants::ActionContext::Registration
  end

  def signin_destination(invitation)
    return sign_in_path if invitation.nil?

    if invitation.credential_delegate?
      confirm_cd_organization_invitation_url(invitation.provider_organization_id, invitation.id, invitation.token)
    else
      accept_organization_invitation_url(invitation.provider_organization_id, invitation.id, invitation.token)
    end
  end
end
