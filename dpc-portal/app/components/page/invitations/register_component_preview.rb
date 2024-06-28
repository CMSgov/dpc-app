# frozen_string_literal: true

module Page
  module Invitations
    # Preview page for registering
    class RegisterComponentPreview < ViewComponent::Preview
      def register_cd
        org = ProviderOrganization.new(id: 2, name: 'Health Hut')
        user = User.new(given_name: 'Robert', family_name: 'Hodges')
        invitation = Invitation.new(id: 4, invited_by: user, invitation_type: :credential_delegate)
        render(Page::Invitations::RegisterComponent.new(org, invitation))
      end

      def register_ao
        org = ProviderOrganization.new(id: 2, name: 'Health Hut')
        user = User.new(given_name: 'Robert', family_name: 'Hodges')
        invitation = Invitation.new(id: 4, invited_by: user, invitation_type: :authorized_official)
        render(Page::Invitations::RegisterComponent.new(org, invitation))
      end
    end
  end
end
