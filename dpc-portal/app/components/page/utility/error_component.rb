# frozen_string_literal: true

module Page
  module Utility
    # Displays unfixable error message in accept invitation process
    class ErrorComponent < ViewComponent::Base
      DISPLAY_NAMES = {
        login_dot_gov: 'Login.gov',
        id_me: 'ID.me'
      }.freeze

      def initialize(invitation, reason, csp: :login_dot_gov)
        super()
        @invitation = invitation
        @org_name = invitation&.provider_organization&.name
        @ao_full_name = invitation&.invited_by_full_name
        @ao_email = invitation&.invited_by&.email
        @csp_display_name = DISPLAY_NAMES.fetch(csp, 'CSP')
        @reason = resolve_reason(reason)
        @status = "verification.#{@reason}_status"
        @text = "verification.#{@reason}_text"
        @show_alert = true if @reason.in?(%i[email_mismatch])
        @alert_type = 'error' if @reason.in?(%i[email_mismatch])
      end

      private

      def resolve_reason(reason)
        AoVerificationService::SERVER_ERRORS.include?(reason) ? :server_error : reason.to_sym
      end
    end
  end
end
