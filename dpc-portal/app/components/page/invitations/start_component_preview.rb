# frozen_string_literal: true

module Page
  module Invitations
    # First page the user sees when accepting a valid invitation
    class StartComponentPreview < ViewComponent::Preview
      def default
        org = ProviderOrganization.new(id: 2, name: 'Health Hut')
        invitation = Invitation.new(id: 4, invited_email: 'doug@example.com', invitation_type: :credential_delegate,
                                    created_at: 47.hours.ago - 59.minutes)
        render(Page::Invitations::StartComponent.new(org, invitation))
      end
    end
  end
end
