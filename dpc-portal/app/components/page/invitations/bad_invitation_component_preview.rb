# frozen_string_literal: true

module Page
  module Invitations
    # Displays unfixable error message in accept invitation process
    class BadInvitationComponentPreview < ViewComponent::Preview
      def ao_invalid_invitation
        invitation = Invitation.new(provider_organization: ProviderOrganization.new(name: 'Health Hut'),
                                    invitation_type: :authorized_official)
        reason = 'invalid'
        render(Page::Invitations::BadInvitationComponent.new(invitation, reason))
      end

      def cd_invalid_invitation
        invitation = Invitation.new(provider_organization: ProviderOrganization.new(name: 'Health Hut'),
                                    invitation_type: :credential_delegate)
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

      def ao_expired
        invitation = Invitation.new(id: 5, provider_organization: ProviderOrganization.new(id: 1, name: 'Health Hut'),
                                    invitation_type: :authorized_official, created_at: 49.hours.ago)
        reason = 'ao_expired'
        render(Page::Invitations::BadInvitationComponent.new(invitation, reason))
      end

      def cd_expired
        invitation = Invitation.new(id: 6, provider_organization: ProviderOrganization.new(id: 1, name: 'Health Hut'),
                                    invitation_type: :credential_delegate, created_at: 49.hours.ago)
        reason = 'cd_expired'
        render(Page::Invitations::BadInvitationComponent.new(invitation, reason))
      end

      def ao_renewed
        invitation = Invitation.new(id: 7, provider_organization: ProviderOrganization.new(id: 1, name: 'Health Hut'),
                                    status: :renewed)
        reason = 'ao_renewed'
        render(Page::Invitations::BadInvitationComponent.new(invitation, reason))
      end

      def cd_accepted
        invitation = Invitation.new(id: 8, provider_organization: ProviderOrganization.new(id: 1, name: 'Health Hut'),
                                    invitation_type: :credential_delegate, status: :renewed)
        reason = 'cd_accepted'
        render(Page::Invitations::BadInvitationComponent.new(invitation, reason))
      end
    end
  end
end
