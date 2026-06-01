# frozen_string_literal: true

module Page
  module Session
    # Previews the log in page
    class LoginComponentPreview < ViewComponent::Preview
      # The really long @param line fails a Rubocop check, but most of the alternatives I tried
      # broke LookBook so I turned the check off.
      # rubocop:disable Layout/LineLength
      # @param last_used_csp select { choices: { "None": "", "CLEAR": "clear", "ID.me": "id_me", "Login.gov": "login_dot_gov" } }
      # rubocop:enable Layout/LineLength
      def default(last_used_csp: nil)
        # Make sure if the user selects "None" that the value passed is actually nil and not "".
        csp_value = last_used_csp.presence

        render(Page::Session::LoginComponent.new(root_path, last_used_csp: csp_value&.to_sym))
      end
    end
  end
end
