# frozen_string_literal: true

module Page
  module Invitations
    # Preview of Invitatation login (IAL/2 flow)
    class InvitationLoginComponentPreview < ViewComponent::Preview
      #
      # @param for_ao toggle
      def parameterized(for_ao: true)
        provider_organization = ProviderOrganization.new(id: 4, name: 'Health Hut')
        invitation_type = for_ao ? :authorized_official : :credential_delegate
        invitation = Invitation.new(id: 2, provider_organization:, invitation_type:)
        render(Page::Invitations::InvitationLoginComponent.new(invitation))
      end
    end
  end
end