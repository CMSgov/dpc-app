# frozen_string_literal: true

module Page
  module ExistingAccount
    # Render the screen to allow for adding a new email
    class AddEmailComponent < ViewComponent::Base
      def initialize(email, csp_code, path)
        super()
        @masked_email = EmailMask.masked(email)
        @display_name = CspUtils.display_name(csp_code)
        @path = path
      end
    end
  end
end
