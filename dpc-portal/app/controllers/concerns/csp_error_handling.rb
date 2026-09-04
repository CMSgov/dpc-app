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

  def handle_invitation_flow_failure(invitation_url, invitation_id)
    if csp_auth_error?
      log_event(:info, 'Failed invitation flow',
                action_context: LoggingConstants::ActionContext::Registration,
                action_type: LoggingConstants::ActionType::FailedLogin,
                invitation: invitation_id)
    end

    redirect_to invitation_url, alert:  "We weren't able to complete identity verification."
  end

  def handle_csp_auth_error
    log_event(:error, 'CSP Authentication error',
              action_context: LoggingConstants::ActionContext::Authentication,
              action_type: LoggingConstants::ActionType::CspUnavailable,
              error: params[:message],
              csp: csp_param)
    render(Page::Utility::ErrorComponent.new(nil, 'server_error', csp: csp_param),
           status: :service_unavailable)
  end

  def handle_signin_fail
    log_event(:error, 'CSP Configuration error',
              action_context: LoggingConstants::ActionContext::Registration,
              action_type: LoggingConstants::ActionType::FailedLogin,
              csp: csp_param)
    render(Page::Utility::ErrorComponent.new(nil, 'csp_signin_fail', csp: csp_param))
  end

  def handle_signin_cancel
    log_event(:info, 'User cancelled login',
              action_context: LoggingConstants::ActionContext::Authentication,
              action_type: LoggingConstants::ActionType::UserCancelledLogin,
              csp: csp_param)
    render(Page::Utility::ErrorComponent.new(nil, 'csp_signin_cancel', csp: csp_param))
  end
end
