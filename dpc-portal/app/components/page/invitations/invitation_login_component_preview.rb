# frozen_string_literal: true

module Page
  module Invitations
    # Preview of Invitation login (IAL/2 flow)
    class InvitationLoginComponentPreview < ViewComponent::Preview
      def ao_login
        provider_organization = ProviderOrganization.new(id: 4, name: 'Health Hut')
        invitation = Invitation.new(id: 2, provider_organization:, invitation_type: :authorized_official)
        render(Page::Invitations::InvitationLoginComponent.new(invitation))
      end

      def cd_login
        provider_organization = ProviderOrganization.new(id: 4, name: 'Health Hut')
        invitation = Invitation.new(id: 2, provider_organization:, invitation_type: :credential_delegate)
        render(Page::Invitations::InvitationLoginComponent.new(invitation))
      end
    end
  end
end
