# frozen_string_literal: true

module Page
  module Invitations
    # Displays unfixable error message in accept invitation process
    class BadInvitationComponent < ViewComponent::Base
      def initialize(invitation, reason)
        super
        @invitation = invitation
        @org_name = invitation&.provider_organization&.name
        @reason = if server_errors.include?(reason)
                    'server_error'
                  else
                    reason
                  end
        @status = "verification.#{@reason}_status"
        @text = "verification.#{@reason}_text"
      end

      def server_errors
        %i[api_gateway_error invalid_endpoint_called unexpected_error]
      end
    end
  end
end
