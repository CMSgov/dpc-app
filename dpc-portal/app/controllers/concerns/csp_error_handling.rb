# frozen_string_literal: true

# Handles errors in CSP flow
module CspErrorHandling
  extend ActiveSupport::Concern

  def handle_invitation_flow_failure(invitation_id)
    log_event(:info, 'Failed invitation flow',
              action_context: LoggingConstants::ActionContext::Registration,
              action_type: LoggingConstants::ActionType::FailedLogin,
              invitation: invitation_id)
    invitation = Invitation.find(invitation_id)
    if invitation.credential_delegate?
      render(Page::Utility::ErrorComponent.new(invitation, 'fail_to_proof'), status: :forbidden)
    else
      render(Page::Invitations::AoFlowFailComponent.new(invitation, 'fail_to_proof', 1), status: :forbidden)
    end
  end

  def handle_signin_fail(csp)
    log_event(:error, 'CSP Configuration error',
              action_context: LoggingConstants::ActionContext::Registration,
              action_type: LoggingConstants::ActionType::FailedLogin,
              csp: csp)
    render(Page::Utility::ErrorComponent.new(nil, 'csp_signin_fail', csp:))
  end

  def handle_signin_cancel(csp)
    log_event(:info, 'User cancelled login',
              action_context: LoggingConstants::ActionContext::Authentication,
              action_type: LoggingConstants::ActionType::UserCancelledLogin,
              csp: csp)
    render(Page::Utility::ErrorComponent.new(nil, 'csp_signin_cancel', csp:))
  end
end
