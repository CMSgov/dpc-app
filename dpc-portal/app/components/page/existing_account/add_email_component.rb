# frozen_string_literal: true

module Page
  module ExistingAccount
    # Render the screen to allow for adding a new email
    class AddEmailComponent < ViewComponent::Base
      def initialize(orig_email, orig_csp)
        super()
        @masked_email = EmailMask.masked(orig_email)
        @orig_csp_code = orig_csp
        @orig_csp_display = CspUtils.display_name(orig_csp)
      end
    end
  end
end
