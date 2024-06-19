# frozen_string_literal: true

module Page
  module Session
    # Preview of Invitatation login (IAL/2 flow)
    class InvitationLoginComponentPreview < ViewComponent::Preview
      def default
        provider_organization = ProviderOrganization.new(id: 4, name: 'Health Hut')
        invitation = Invitation.new(id: 2, provider_organization:)
        render(Page::Session::InvitationLoginComponent.new(invitation))
      end
    end
  end
end
