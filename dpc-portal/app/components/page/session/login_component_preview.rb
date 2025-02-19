# frozen_string_literal: true

module Page
  module Session
    # Previews the log in page
    class LoginComponentPreview < ViewComponent::Preview
      def default
        render(Page::Session::LoginComponent.new(root_path))
      end
    end
  end
end
