# frozen_string_literal: true

module Page
  module Invitations
    # Displays unfixable error message in accept invitation process
    class BadInvitationComponentPreview < ViewComponent::Preview
      def invalid_invitation
        invitation = Invitation.new(provider_organization: ProviderOrganization.new(name: 'Health Hut'))
        reason = 'invalid'
        render(Page::Invitations::BadInvitationComponent.new(invitation, reason))
      end

      def pii_mismatch
        invitation = Invitation.new(provider_organization: ProviderOrganization.new(name: 'Health Hut'))
        reason = 'pii_mismatch'
        render(Page::Invitations::BadInvitationComponent.new(invitation, reason))
      end

      # @param error_code
      def verification_failure(error_code: :user_not_authorized_official)
        invitation = Invitation.new(id: 3, provider_organization: ProviderOrganization.new(id: 1, name: 'Health Hut'))
        render(Page::Invitations::BadInvitationComponent.new(invitation, error_code))
      end
    end
  end
end
