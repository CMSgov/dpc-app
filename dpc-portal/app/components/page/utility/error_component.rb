# frozen_string_literal: true

module Page
  module Utility
    # Displays unfixable error message in accept invitation process
    class ErrorComponent < ViewComponent::Base
      DISPLAY_NAMES = {
        login_dot_gov: 'Login.gov',
        id_me: 'ID.me'
      }.freeze

      def initialize(invitation, reason, csp)
        super()
        @invitation = invitation
        @org_name = invitation&.provider_organization&.name
        @ao_full_name = invitation&.invited_by_full_name
        @ao_email = invitation&.invited_by&.email
        @csp_display_name = DISPLAY_NAMES.fetch(csp, 'CSP')
        @reason = resolve_reason(reason, csp)
        @status = "verification.#{@reason}_status"
        @text = "verification.#{@reason}_text"
      end

      private

      def resolve_reason(reason, csp)
        return :server_error if AoVerificationService::SERVER_ERRORS.include?(reason)
        return reason.to_sym unless reason.start_with?('csp') && DISPLAY_NAMES.key?(csp)

        reason.sub! 'csp', csp
      end
    end
  end
end
