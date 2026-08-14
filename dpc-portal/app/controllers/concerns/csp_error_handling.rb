# frozen_string_literal: true

# Handles errors in CSP flow
module CspErrorHandling
  extend ActiveSupport::Concern

  CSP_AUTH_ERROR_MESSAGES = %w[server_error service_unavailable connection_failed internal_server_error timeout].freeze
  CSP_USER_ERROR_MESSAGES = %w[access_denied].freeze

  def csp_auth_error?
    CSP_AUTH_ERROR_MESSAGES.include?(params[:message])
  end

  def csp_user_error?
    CSP_USER_ERROR_MESSAGES.include?(params[:message])
  end

  def csp_param
    params[:strategy]
  end

  def handle_invitation_flow_failure(invitation_id)
    Rails.logger.info(['Failed invitation flow',
                       { actionContext: LoggingConstants::ActionContext::Registration,
                         actionType: LoggingConstants::ActionType::FailedLogin,
                         **csp_log_context }])
    invitation = Invitation.find(invitation_id)
    if invitation.credential_delegate?
      render(Page::Utility::ErrorComponent.new(invitation, 'fail_to_proof'), status: :forbidden)
    else
      render(Page::Invitations::AoFlowFailComponent.new(invitation, 'fail_to_proof', 1), status: :forbidden)
    end
  end

  def handle_csp_auth_error
    Rails.logger.error(['CSP Authentication error',
                        { actionContext: LoggingConstants::ActionContext::Authentication,
                          actionType: LoggingConstants::ActionType::CspUnavailable,
                          error: params[:message],
                          csp: csp_param }])

    render(Page::Utility::ErrorComponent.new(nil, 'server_error', csp: csp_param),
           status: :service_unavailable)
  end

  def handle_signin_fail
    Rails.logger.error 'CSP Configuration error'
    render(Page::Utility::ErrorComponent.new(nil, 'csp_signin_fail', csp: csp_param))
  end

  def handle_signin_cancel
    Rails.logger.info(['User cancelled login',
                       { actionContext: LoggingConstants::ActionContext::Authentication,
                         actionType: LoggingConstants::ActionType::UserCancelledLogin,
                         csp: csp_param }])
    render(Page::Utility::ErrorComponent.new(nil, 'csp_signin_cancel', csp: csp_param))
  end
end
