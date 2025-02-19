# frozen_string_literal: true

module Page
  module Invitations
    # Displays unfixable error message in accept invitation process
    class BadInvitationComponent < ViewComponent::Base
      def initialize(invitation, reason)
        super
        @invitation = invitation
        @org_name = invitation&.provider_organization&.name
        @ao_full_name = invitation&.invited_by_full_name
        @ao_email = invitation&.invited_by&.email
        @reason = if AoVerificationService::SERVER_ERRORS.include?(reason)
                    :server_error
                  else
                    reason.to_sym
                  end
        @status = "verification.#{@reason}_status"
        @text = "verification.#{@reason}_text"
      end
    end
  end
end
