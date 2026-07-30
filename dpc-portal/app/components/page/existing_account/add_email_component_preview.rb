# frozen_string_literal: true

module Page
  module ExistingAccount
    # Render the screen to add a new email to an existing account.
    class AddEmailComponentPreview < ViewComponent::Preview
      def default
        render(Page::ExistingAccount::AddEmailComponent.new('bob@example.com', :login_dot_gov))
      end
    end
  end
end
