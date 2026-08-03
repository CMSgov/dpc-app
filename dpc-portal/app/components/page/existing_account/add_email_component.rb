# frozen_string_literal: true

module Page
  module ExistingAccount
    # Render the screen to allow for adding a new email
    class AddEmailComponent < ViewComponent::Base
      def initialize(orig_email, orig_csp)
        super()
        @orig_email = orig_email
        @orig_csp_code = orig_csp
        @orig_csp_display = display_name(orig_csp)
      end

      private

      def display_name(csp_code)
        case csp_code.to_sym
        when :login_dot_gov
          'Login.gov'
        when :id_me
          'ID.me'
        when :clear
          'CLEAR'
        else
          'CSP'
        end
      end
    end
  end
end
