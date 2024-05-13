# frozen_string_literal: true

module Page
  module Invitations
    # Displays unfixable error message in accept invitation process
    class BadInvitationComponent < ViewComponent::Base
      def initialize(reason, level)
        super
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
      end

      def server_errors
        %i[api_gateway_error invalid_endpoint_called unexpected_error]
      end
    end
  end
end
