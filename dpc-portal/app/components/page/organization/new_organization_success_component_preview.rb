# frozen_string_literal: true

module Page
  module Organization
    # Previews Invite Credential Delegate Success Page
    class NewOrganizationSuccessComponentPreview < ViewComponent::Preview
      def default
        org = ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2)
        render(Page::Organization::NewOrganizationSuccessComponent.new(org))
      end
    end
  end
end
