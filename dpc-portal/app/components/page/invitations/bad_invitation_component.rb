# frozen_string_literal: true

module Page
  module Invitations
    # Displays unfixable error message in accept invitation process
    class BadInvitationComponent < ViewComponent::Base
      def initialize(invitation, reason)
        super
        @invitation = invitation
        @org_name = invitation&.provider_organization&.name
        @reason = if AoVerificationService::SERVER_ERRORS.include?(reason)
                    :server_error
                  else
                    reason.to_sym
                  end
        @status = get_status(invitation, reason)
        @text = "verification.#{@reason}_text"
      end

      private

      def get_status(invitation, reason)
        if reason == 'invalid' && invitation.authorized_official?
          'verification.ao_invalid_status'
        elsif reason == 'invalid' && invitation.credential_delegate?
          'verification.cd_invalid_status'
        else
          "verification.#{@reason}_status"
        end
      end
    end
  end
end
