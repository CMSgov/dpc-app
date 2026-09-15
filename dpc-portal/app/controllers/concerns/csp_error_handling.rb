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
    handle_error_redirects(invitation, 'server_error')
  end

  def handle_signin_fail(invitation)
    log_event(:error, 'CSP Configuration error',
              action_context: LoggingConstants::ActionContext::Registration,
              action_type: LoggingConstants::ActionType::FailedLogin,
              csp: csp_param)
    handle_error_redirects(invitation, 'csp_signin_fail')
  end

  def handle_signin_cancel(invitation)
    log_event(:info, 'User cancelled login',
              action_context: LoggingConstants::ActionContext::Authentication,
              action_type: LoggingConstants::ActionType::UserCancelledLogin,
              csp: csp_param)
    handle_error_redirects(invitation, 'csp_signin_cancel')
  end

  # rubocop:disable-next Metrics/AbcSize
  def handle_error_redirects(invitation, error_display_text)
    # TODO: pass in alert text specific to each error type in upcoming task
    alert_text = "We weren't able to complete identity verification."

    # TODO: redirect nil invitations to main sign in page in upcoming task
    if invitation.nil? && (error_display_text == 'server_error')
      render(Page::Utility::ErrorComponent.new(nil, error_display_text, csp: csp_param), status: :service_unavailable)
    elsif invitation.nil?
      render(Page::Utility::ErrorComponent.new(nil, error_display_text, csp: csp_param))
    elsif invitation.credential_delegate?
      redirect_to confirm_cd_organization_invitation_url(invitation.provider_organization_id,
                                                         invitation.id,
                                                         invitation.token),
                  alert: alert_text
    else
      redirect_to accept_organization_invitation_url(invitation.provider_organization_id,
                                                     invitation.id,
                                                     invitation.token),
                  alert: alert_text
    end
  end
end
