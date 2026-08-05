# frozen_string_literal: true

module Page
  module ExistingAccount
    # Render the screen to add a new email to an existing account.
    class AddEmailComponentPreview < ViewComponent::Preview
      def default
        org = ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2)
        render(Page::ExistingAccount::AddEmailComponent.new('bob@example.com', :login_dot_gov, organization_path(org)))
      end
    end
  end
end
