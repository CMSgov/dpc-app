# frozen_string_literal: true

module Page
  module ExistingAccount
    # Render the screen to add a new CSP
    class LinkAccountComponent < ViewComponent::Base
      def initialize(email, csp_code)
        super()
        @masked_email = EmailMask.masked(email)
        @csp_code = csp_code
        @display_name = CspUtils.display_name(csp_code)
      end
    end
  end
end
