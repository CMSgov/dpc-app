# frozen_string_literal: true

module Page
  module CredentialDelegate
    class AcceptInvitationComponentPreview < ViewComponent::Preview
      def accept
        org = ProviderOrganization.new(id: 2, name: 'Health Hut')
        user = User.new(given_name: 'Robert', family_name: 'Hodges')
        invitation = Invitation.new(id: 4, invited_by: user)
        render(Page::CredentialDelegate::AcceptInvitationComponent.new(org, invitation))
      end
      def error
        org = ProviderOrganization.new(id: 2, name: 'Health Hut')
        user = User.new(given_name: 'Robert', family_name: 'Hodges')
        invitation = Invitation.new(id: 4, invited_by: user)
        invitation.errors.add(:verification_code, :not_right, message: 'Helpful error message')
        render(Page::CredentialDelegate::AcceptInvitationComponent.new(org, invitation))
      end
    end
  end
end
