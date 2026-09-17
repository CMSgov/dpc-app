# frozen_string_literal: true

module Page
  module Utility
    # Displays failure to proof page
    class VerificationFailureComponent < ViewComponent::Base
      DISPLAY_NAMES = {
        login_dot_gov: 'Login.gov',
        id_me: 'ID.me',
        clear: 'CLEAR'
      }.freeze

      def initialize(invitation, csp: nil)
        super()
        @invitation = invitation
        @current_csp = csp&.to_sym
        @csp_display_name = DISPLAY_NAMES.fetch(csp&.to_sym, 'CSP')
      end

      def sign_in_destination
        if @invitation.nil?
          sign_in_path
        elsif @invitation.credential_delegate?
          confirm_cd_organization_invitation_path(@invitation.provider_organization_id,
                                                  @invitation.id,
                                                  @invitation.token)
        else
          accept_organization_invitation_path(@invitation.provider_organization_id, @invitation.id, @invitation.token)
        end
      end
    end
  end
end
