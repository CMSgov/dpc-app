# frozen_string_literal: true

module Page
  module Session
    # Renders the log in page
    class LoginComponent < ViewComponent::Base
      def initialize(idme_login_path, clear_login_path)
        super
        @login_path = idme_login_path
        @clear_login_path = clear_login_path
      end
    end
  end
end
