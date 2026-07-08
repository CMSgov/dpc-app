# frozen_string_literal: true

module Page
  module Utility
    # Displays unfixable error message in accept invitation process
    class ErrorComponentPreview < ViewComponent::Preview
      DEFAULT_CSP = :login_dot_gov
      def ao_invalid
        invitation = Invitation.new(provider_organization: ProviderOrganization.new(name: 'Health Hut'))
        reason = 'ao_invalid'
        render(Page::Utility::ErrorComponent.new(invitation, reason))
      end

      def cd_invalid
        invitation = Invitation.new(provider_organization: ProviderOrganization.new(name: 'Health Hut'))
        reason = 'cd_invalid'
        render(Page::Utility::ErrorComponent.new(invitation, reason))
      end

      # @param csp select :csp_codes
      def pii_mismatch(csp: DEFAULT_CSP)
        user = User.new(email: 'bilbo.baggins@cms.hms.gov')
        invitation = Invitation.new(provider_organization: ProviderOrganization.new(name: 'Health Hut'),
                                    invited_by: user)
        reason = 'pii_mismatch'
        render(Page::Utility::ErrorComponent.new(invitation, reason, csp:))
      end

      # @param csp select :csp_codes
      def email_mismatch(csp: DEFAULT_CSP)
        invitation = Invitation.new(provider_organization: ProviderOrganization.new(name: 'Health Hut'))
        reason = 'email_mismatch'
        render(Page::Utility::ErrorComponent.new(invitation, reason, csp:))
      end

      # @param error_code select :error_codes
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

      # @param csp select :csp_codes
      def ao_accepted(csp: DEFAULT_CSP)
        invitation = Invitation.new(id: 9, provider_organization: ProviderOrganization.new(id: 1, name: 'Health Hut'),
                                    invitation_type: :authorized_official, status: :renewed)
        reason = 'ao_accepted'
        render(Page::Utility::ErrorComponent.new(invitation, reason, csp:))
      end

      def server_error
        invitation = Invitation.new(provider_organization: ProviderOrganization.new(name: 'Health Hut'))
        reason = 'server_error'
        render(Page::Utility::ErrorComponent.new(invitation, reason))
      end

      # @param csp select :csp_codes
      def no_account(csp: DEFAULT_CSP)
        reason = 'no_account'
        render(Page::Utility::ErrorComponent.new(nil, reason, csp:))
      end

      # @param csp select :csp_codes
      def csp_signin_cancel(csp: DEFAULT_CSP)
        reason = 'csp_signin_cancel'
        render(Page::Utility::ErrorComponent.new(nil, reason, csp:))
      end

      # @param csp select :csp_codes
      def csp_signin_fail(csp: DEFAULT_CSP)
        reason = 'csp_signin_fail'
        render(Page::Utility::ErrorComponent.new(nil, reason, csp:))
      end

      private

      def csp_codes
        { choices: %i[login_dot_gov id_me] }
      end

      def error_codes
        { choices: %i[user_not_authorized_official no_approved_enrollment bad_npi org_med_sanctions ao_med_sanctions
                      missing_info server_error fail_to_proof] }
      end
    end
  end
end
