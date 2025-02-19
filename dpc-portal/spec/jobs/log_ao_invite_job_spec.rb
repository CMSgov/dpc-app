# frozen_string_literal: true

require 'rails_helper'

RSpec.describe LogAoInviteJob, type: :job do
  include ActiveJob::TestHelper

  describe :perform do
    it 'logs invitation' do
      invitation = create(:invitation, :ao)
      expect(CurrentAttributes).to receive(:save_organization_attributes).with(invitation.provider_organization, nil)
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info).with(['Authorized Official invited',
                                                   { actionContext: LoggingConstants::ActionContext::Registration,
                                                     actionType: LoggingConstants::ActionType::AoInvited,
                                                     invitation: invitation.id }])
      LogAoInviteJob.perform_now(invitation.id)
    end
    it 'logs failure to log' do
      bad_id = 'foo'
      message = "Unable to log Authorized official creation: no Invitation with id: #{bad_id}"
      expect(Rails.logger).to receive(:error).with(message)

      LogAoInviteJob.perform_now(bad_id)
    end
  end
end
