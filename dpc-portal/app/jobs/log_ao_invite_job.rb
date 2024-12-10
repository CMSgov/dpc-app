# frozen_string_literal: true


# Logs AO Invitation creation
# Actions activated by rake tasks on AWS servers are not logging to CloudWatch, so we need
# to have sidekiq do the logging, since its logs seem to go through
class LogAoInviteJob < ApplicationJob
  queue_as :portal

  def perform(invitation_id)
    invitation = Invitation.find(invitation_id)
    CurrentAttributes.save_organization_attributes(invitation.provider_organization, nil)
    Rails.logger.info(['Authorized Official invited',
                       { actionContext: LoggingConstants::ActionContext::Registration,
                         actionType: LoggingConstants::ActionType::AoInvited,
                         invitation: invitation_id }])
  end
end
