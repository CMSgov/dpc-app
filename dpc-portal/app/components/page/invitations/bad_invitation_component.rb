# frozen_string_literal: true

module Page
  module Invitations
    # Displays unfixable error message in accept invitation process
    class BadInvitationComponent < ViewComponent::Base
      def initialize(invitation, reason, level)
        super
        @invitation = invitation
        @org_name = invitation&.provider_organization&.name
        @reason = if server_errors.include?(reason)
                    'server_error'
                  else
                    reason
                  end
        @level = level
        @heading = "verification.#{@reason}_heading"
        @status = "verification.#{@reason}_status"
        @alert = "verification.#{@reason}_alert"
        @text = "verification.#{@reason}_text"
        @show_renew = reason.to_sym == :ao_expired
        @disabled = invitation&.renewed?
      end

      def server_errors
        %i[api_gateway_error invalid_endpoint_called unexpected_error]
      end
    end
  end
end
