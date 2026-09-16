# frozen_string_literal: true

# Handles errors in CSP flow
module CspErrorHandling
  extend ActiveSupport::Concern

  CSP_AUTH_ERROR_MESSAGES = %w[server_error service_unavailable connection_failed internal_server_error timeout].freeze
  CSP_USER_ERROR_MESSAGES = %w[access_denied].freeze

  def csp_auth_error?
    CSP_AUTH_ERROR_MESSAGES.include?(params[:message])
  end

  def csp_user_cancelled?
    CSP_USER_ERROR_MESSAGES.include?(params[:message])
  end

  def csp_param
    params[:strategy] || csp_session.current
  end

  def handle_csp_auth_error(invitation)
    log_event(:error, 'CSP Authentication error',
              action_context: LoggingConstants::ActionContext::Authentication,
              action_type: LoggingConstants::ActionType::CspUnavailable,
              error: params[:message],
              csp: csp_param)
    render_or_redirect(invitation, 'server_error')
  end

  def handle_signin_fail(invitation)
    log_event(:error, 'CSP Configuration error',
              action_context: LoggingConstants::ActionContext::Registration,
              action_type: LoggingConstants::ActionType::FailedLogin,
              csp: csp_param)
    render_or_redirect(invitation, 'csp_signin_fail')
  end

  def handle_signin_cancel(invitation)
    log_event(:info, 'User cancelled login',
              action_context: LoggingConstants::ActionContext::Authentication,
              action_type: LoggingConstants::ActionType::UserCancelledLogin,
              csp: csp_param)
    render_or_redirect(invitation, 'csp_signin_cancel')
  end

  def render_or_redirect(invitation, error_reason)
    return render(Page::Utility::ErrorComponent.new(nil, error_reason, csp: csp_param)) if invitation.nil?

    redirect_to redirect_url(invitation), alert: "We weren't able to complete identity verification."
  end

  private

  def action_context(invitation)
    invitation.nil? ? LoggingConstants::ActionContext::Authentication : LoggingConstants::ActionContext::Registration
  end

  def redirect_url(invitation)
    if invitation.credential_delegate?
      confirm_cd_organization_invitation_url(invitation.provider_organization_id, invitation.id, invitation.token)
    else
      accept_organization_invitation_url(invitation.provider_organization_id, invitation.id, invitation.token)
    end
  end
end
