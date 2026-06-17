# frozen_string_literal: true

# Handles errors in CSP flow
module CspErrorHandling
  extend ActiveSupport::Concern

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

  def handle_signin_fail(csp)
    Rails.logger.error 'CSP Configuration error'
    render(Page::Utility::ErrorComponent.new(nil, "#{csp || 'csp'}_signin_fail"))
  end

  def handle_signin_cancel(csp)
    Rails.logger.info(['User cancelled login',
                       { actionContext: LoggingConstants::ActionContext::Authentication,
                         actionType: LoggingConstants::ActionType::UserCancelledLogin,
                         **csp_log_context }])
    render(Page::Utility::ErrorComponent.new(nil, "#{csp || 'csp'}_signin_cancel"))
  end
end
