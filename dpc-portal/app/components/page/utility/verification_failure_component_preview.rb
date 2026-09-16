# frozen_string_literal: true

module Page
  module Utility
    # Displays failure to proof page
    class VerificationFailureComponentPreview < ViewComponent::Preview
      DEFAULT_CSP = :login_dot_gov

      # @param csp select :csp_codes
      def main_sign_in(csp: DEFAULT_CSP)
        render(Page::Utility::VerificationFailureComponent.new(nil, csp:))
      end

      private

      def csp_codes
        { choices: %i[login_dot_gov id_me] }
      end
    end
  end
end
