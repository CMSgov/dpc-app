# frozen_string_literal: true

module Page
  module Utility
    # Displays failure to proof page
    class VerificationFailureComponent < ViewComponent::Base
      DISPLAY_NAMES = {
        login_dot_gov: 'Login.gov',
        id_me: 'ID.me',
        clear: 'CLEAR'
      }.freeze

      def initialize(invitation, csp)
        super()
        @invitation = invitation
        @current_csp = csp.to_sym
        @csp_display_name = DISPLAY_NAMES[csp.to_sym]
      end
    end
  end
end
