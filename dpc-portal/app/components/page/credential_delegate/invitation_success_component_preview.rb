# frozen_string_literal: true

module Page
  module CredentialDelegate
    # Previews Invite Credential Delegate Success Page
    class InvitationSuccessComponentPreview < ViewComponent::Preview
      def default
        org = ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2)
        render(Page::CredentialDelegate::InvitationSuccessComponent.new(org))
      end
    end
  end
end
