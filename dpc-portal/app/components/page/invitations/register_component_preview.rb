# frozen_string_literal: true

module Page
  module Invitations
    # Preview page for registering
    class RegisterComponentPreview < ViewComponent::Preview
      def default
        org = ProviderOrganization.new(id: 2, name: 'Health Hut')
        user = User.new(given_name: 'Robert', family_name: 'Hodges')
        invitation = Invitation.new(id: 4, invited_by: user, invitation_type: :authorized_official)
        render(Page::Invitations::RegisterComponent.new(org, invitation))
      end
    end
  end
end
