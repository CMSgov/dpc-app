# frozen_string_literal: true

module Page
  module Invitations
    # Displays accept invitation form
    class AcceptInvitationComponentPreview < ViewComponent::Preview
      def accept_ao
        org = ProviderOrganization.new(id: 2, name: 'Health Hut')
        user = User.new(given_name: 'Robert', family_name: 'Hodges')
        invitation = Invitation.new(id: 4, invited_by: user, invitation_type: :authorized_official)
        render(Page::Invitations::AcceptInvitationComponent.new(org, invitation, 'Paula', 'Pineiro'))
      end

      def accept_cd
        org = ProviderOrganization.new(id: 2, name: 'Health Hut')
        user = User.new(given_name: 'Robert', family_name: 'Hodges')
        invitation = Invitation.new(id: 4, invited_by: user, invitation_type: :credential_delegate)
        render(Page::Invitations::AcceptInvitationComponent.new(org, invitation, 'Paula', 'Pineiro'))
      end
    end
  end
end
