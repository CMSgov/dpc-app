# frozen_string_literal: true

module Page
  module Session
    # Renders the log in page
    class LoginComponent < ViewComponent::Base
      def initialize(login_path)
        super
        @login_path = login_path
      end
    end
  end
end
