# frozen_string_literal: true

module Page
  module Session
    # Renders the log in page
    class LoginComponent < ViewComponent::Base
      def initialize(login_path, last_used_csp: nil)
        super()
        @login_path = login_path
        @last_used_csp = last_used_csp
      end
    end
  end
end
