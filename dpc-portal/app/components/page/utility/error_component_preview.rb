# frozen_string_literal: true

module Page
  module Utility
    # Displays unfixable error message in accept invitation process
    class ErrorComponentPreview < ViewComponent::Preview
      def invalid_invitation
        invitation = Invitation.new(provider_organization: ProviderOrganization.new(name: 'Health Hut'))
        reason = 'invalid'
        render(Page::Utility::ErrorComponent.new(invitation, reason))
      end

      def pii_mismatch
        user = User.new(email: 'bilbo.baggins@cms.hms.gov')
        invitation = Invitation.new(provider_organization: ProviderOrganization.new(name: 'Health Hut'),
                                    invited_by: user)
        reason = 'pii_mismatch'
        render(Page::Utility::ErrorComponent.new(invitation, reason))
      end

      def email_mismatch
        invitation = Invitation.new(provider_organization: ProviderOrganization.new(name: 'Health Hut'))
        reason = 'email_mismatch'
        render(Page::Utility::ErrorComponent.new(invitation, reason))
      end

      # @param error_code
      def verification_failure(error_code: :user_not_authorized_official)
        invitation = Invitation.new(id: 3, provider_organization: ProviderOrganization.new(id: 1, name: 'Health Hut'))
        render(Page::Utility::ErrorComponent.new(invitation, error_code))
      end

      def ao_expired
        invitation = Invitation.new(id: 5, provider_organization: ProviderOrganization.new(id: 1, name: 'Health Hut'),
                                    invitation_type: :authorized_official, created_at: 49.hours.ago)
        reason = 'ao_expired'
        render(Page::Utility::ErrorComponent.new(invitation, reason))
      end

      def cd_expired
        user = User.new(email: 'bilbo.baggins@cms.hms.gov')
        invitation = Invitation.new(id: 6, provider_organization: ProviderOrganization.new(id: 1, name: 'Health Hut'),
                                    invited_by: user, invitation_type: :credential_delegate, created_at: 49.hours.ago)
        reason = 'cd_expired'
        render(Page::Utility::ErrorComponent.new(invitation, reason))
      end

      def ao_renewed
        invitation = Invitation.new(id: 7, provider_organization: ProviderOrganization.new(id: 1, name: 'Health Hut'),
                                    status: :renewed)
        reason = 'ao_renewed'
        render(Page::Utility::ErrorComponent.new(invitation, reason))
      end

      def cd_accepted
        invitation = Invitation.new(id: 8, provider_organization: ProviderOrganization.new(id: 1, name: 'Health Hut'),
                                    invitation_type: :credential_delegate, status: :renewed)
        reason = 'cd_accepted'
        render(Page::Utility::ErrorComponent.new(invitation, reason))
      end

      def server_error
        invitation = Invitation.new(provider_organization: ProviderOrganization.new(name: 'Health Hut'))
        reason = 'server_error'
        render(Page::Utility::ErrorComponent.new(invitation, reason))
      end

      def no_account
        reason = 'no_account'
        render(Page::Utility::ErrorComponent.new(nil, reason))
      end
      def login_gov_signin_cancel
        reason = 'login_gov_signin_cancel'
        render(Page::Utility::ErrorComponent.new(nil, reason))
      end
      def login_gov_signin_fail
        reason = 'login_gov_signin_fail'
        render(Page::Utility::ErrorComponent.new(nil, reason))
      end
    end
  end
end
