# frozen_string_literal: true

module Page
  module Utility
    # Displays unfixable error message in accept invitation process
    class ErrorComponentPreview < ViewComponent::Preview
      # @param csp select { choices: [:login_dot_gov, :id_me] }
      def invalid_invitation(csp)
        invitation = Invitation.new(provider_organization: ProviderOrganization.new(name: 'Health Hut'))
        reason = 'invalid'
        render(Page::Utility::ErrorComponent.new(invitation, reason, csp))
      end

      # @param csp select { choices: [:login_dot_gov, :id_me] }
      def pii_mismatch(csp)
        user = User.new(email: 'bilbo.baggins@cms.hms.gov')
        invitation = Invitation.new(provider_organization: ProviderOrganization.new(name: 'Health Hut'),
                                    invited_by: user)
        reason = 'pii_mismatch'
        render(Page::Utility::ErrorComponent.new(invitation, reason, csp))
      end

      # @param csp select { choices: [:login_dot_gov, :id_me] }
      def email_mismatch(csp)
        invitation = Invitation.new(provider_organization: ProviderOrganization.new(name: 'Health Hut'))
        reason = 'email_mismatch'
        render(Page::Utility::ErrorComponent.new(invitation, reason, csp))
      end

      # @param error_code
      # @param csp select { choices: [:login_dot_gov, :id_me] }
      def verification_failure(csp, error_code: :user_not_authorized_official)
        invitation = Invitation.new(id: 3, provider_organization: ProviderOrganization.new(id: 1, name: 'Health Hut'))
        render(Page::Utility::ErrorComponent.new(invitation, error_code, csp))
      end

      # @param csp select { choices: [:login_dot_gov, :id_me] }
      def ao_expired(csp)
        invitation = Invitation.new(id: 5, provider_organization: ProviderOrganization.new(id: 1, name: 'Health Hut'),
                                    invitation_type: :authorized_official, created_at: 49.hours.ago)
        reason = 'ao_expired'
        render(Page::Utility::ErrorComponent.new(invitation, reason, csp))
      end

      # @param csp select { choices: [:login_dot_gov, :id_me] }
      def cd_expired(csp)
        user = User.new(email: 'bilbo.baggins@cms.hms.gov')
        invitation = Invitation.new(id: 6, provider_organization: ProviderOrganization.new(id: 1, name: 'Health Hut'),
                                    invited_by: user, invitation_type: :credential_delegate, created_at: 49.hours.ago)
        reason = 'cd_expired'
        render(Page::Utility::ErrorComponent.new(invitation, reason, csp))
      end

      # @param csp select { choices: [:login_dot_gov, :id_me] }
      def ao_renewed(csp)
        invitation = Invitation.new(id: 7, provider_organization: ProviderOrganization.new(id: 1, name: 'Health Hut'),
                                    status: :renewed)
        reason = 'ao_renewed'
        render(Page::Utility::ErrorComponent.new(invitation, reason, csp))
      end

      # @param csp select { choices: [:login_dot_gov, :id_me] }
      def cd_accepted(csp)
        invitation = Invitation.new(id: 8, provider_organization: ProviderOrganization.new(id: 1, name: 'Health Hut'),
                                    invitation_type: :credential_delegate, status: :renewed)
        reason = 'cd_accepted'
        render(Page::Utility::ErrorComponent.new(invitation, reason, csp))
      end

      # @param csp select { choices: [:login_dot_gov, :id_me] }
      def ao_accepted(csp)
        invitation = Invitation.new(id: 9, provider_organization: ProviderOrganization.new(id: 1, name: 'Health Hut'),
                                    invitation_type: :authorized_official, status: :renewed)
        reason = 'ao_accepted'
        render(Page::Utility::ErrorComponent.new(invitation, reason, csp))
      end

      # @param csp select { choices: [:login_dot_gov, :id_me] }
      def server_error(csp)
        invitation = Invitation.new(provider_organization: ProviderOrganization.new(name: 'Health Hut'))
        reason = 'server_error'
        render(Page::Utility::ErrorComponent.new(invitation, reason, csp))
      end

      # @param csp select { choices: [:login_dot_gov, :id_me] }
      def no_account(csp)
        reason = 'no_account'
        render(Page::Utility::ErrorComponent.new(nil, reason, csp))
      end

      # @param csp select { choices: [:login_dot_gov, :id_me] }
      def csp_signin_cancel(csp)
        reason = "#{csp}_signin_cancel"
        render(Page::Utility::ErrorComponent.new(nil, reason, csp))
      end

      # @param csp select { choices: [:login_dot_gov, :id_me] }
      def csp_signin_fail(csp)
        reason = "#{csp}_signin_fail"
        render(Page::Utility::ErrorComponent.new(nil, reason, csp))
      end
    end
  end
end
