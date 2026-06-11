# frozen_string_literal: true

module Page
  module Utility
    # Displays unfixable error message in accept invitation process
    class ErrorComponent < ViewComponent::Base
      def initialize(invitation, reason)
        super()
        @invitation = invitation
        @org_name = invitation&.provider_organization&.name
        @ao_full_name = invitation&.invited_by_full_name
        @ao_email = invitation&.invited_by&.email
        @reason = resolve_reason(reason)
        @status = "verification.#{@reason}_status"
        @text = "verification.#{@reason}_text"
      end

      private

      def resolve_reason(reason)
        AoVerificationService::SERVER_ERRORS.include?(reason) ? :server_error : reason.to_sym
      end
    end
  end
end
