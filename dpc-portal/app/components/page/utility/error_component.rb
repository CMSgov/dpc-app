# frozen_string_literal: true

module Page
  module Utility
    # Displays unfixable error message in accept invitation process
    class ErrorComponent < ViewComponent::Base
      # TODO: remove default -acw
      def initialize(invitation, reason, csp: :login_dot_gov)
        super()
        @invitation = invitation
        @org_name = invitation&.provider_organization&.name
        @ao_full_name = invitation&.invited_by_full_name
        @ao_email = invitation&.invited_by&.email
        @reason = if AoVerificationService::SERVER_ERRORS.include?(reason)
                    :server_error
                  else
                    reason.to_sym
                  end
        @csp_display_name = case csp.to_s
                            when 'login_dot_gov' then 'Login.gov'
                            when 'id_me' then 'ID.me'
                            else 'CSP'
                            end
        @status = "verification.#{@reason}_status"
        @text = "verification.#{@reason}_text"
      end
    end
  end
end
