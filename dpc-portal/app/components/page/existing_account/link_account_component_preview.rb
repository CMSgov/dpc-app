# frozen_string_literal: true

module Page
  module ExistingAccount
    # Render the screen to add a new CSP to an existing account.
    class LinkAccountComponentPreview < ViewComponent::Preview
      def default
        render(Page::ExistingAccount::LinkAccountComponent.new('bobhoskins@example.com', :login_dot_gov))
      end
    end
  end
end
