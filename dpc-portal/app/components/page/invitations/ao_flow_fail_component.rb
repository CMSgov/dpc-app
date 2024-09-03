# frozen_string_literal: true

module Page
  module Invitations
    # Displays unfixable error message in accept invitation process
    class AoFlowFailComponent < ViewComponent::Base
      def initialize(invitation, reason, step)
        super
        @invitation = invitation
        @step = step.to_i
        @org_name = invitation&.provider_organization&.name
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
