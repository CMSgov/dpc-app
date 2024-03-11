# frozen_string_literal: true

module Page
  module Organization
    # Preview of Tos Acceptance Form
    class TosFormComponentPreview < ViewComponent::Preview
      def default
        organization = ProviderOrganization.new(npi: '1111111111', name: 'Health Hut')
        render(Page::Organization::TosFormComponent.new(organization))
      end
    end
  end
end
