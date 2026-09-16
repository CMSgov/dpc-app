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
    handle_csp_error(level: :error,
                     alert_text: 'CSP Authentication error',
                     action_type: LoggingConstants::ActionType::CspUnavailable,
                     invitation:,
                     error: params[:message])
    render_or_redirect(invitation, 'server_error')
  end

  def handle_signin_fail(invitation)
    handle_csp_error(level: :error,
                     alert_text: 'CSP Configuration error',
                     action_type: LoggingConstants::ActionType::FailedLogin,
                     invitation:,
                     error: params[:message])
    render_or_redirect(invitation, 'csp_signin_fail')
  end

  def handle_signin_cancel(invitation)
    handle_csp_error(level: :info,
                     alert_text: 'User cancelled login',
                     action_type: LoggingConstants::ActionType::UserCancelledLogin,
                     invitation:)
    render_or_redirect(invitation, 'csp_signin_cancel')
  end

  def render_or_redirect(invitation, error_reason)
    return render(Page::Utility::ErrorComponent.new(nil, error_reason, csp: csp_param)) if invitation.nil?

    redirect_to redirect_url(invitation), alert: "We weren't able to complete identity verification."
  end

  private

  def handle_csp_error(level:, alert_text:, action_type:, invitation:, error: nil)
    log_event(level, alert_text,
              action_context: action_context(invitation),
              action_type: action_type,
              csp: csp_param,
              **{ error: }.compact)
  end

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
