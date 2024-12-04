# frozen_string_literal: true

module Page
  module Organization
    # Shows tabbed credential delegates and credentials
    class CompoundShowComponentPreview < ViewComponent::Preview
      def authorized_official
        org = ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2)
        render(Page::Organization::CompoundShowComponent.new(org, { active: [], pending: [], expired: [] }))
      end

      def credential_delegate
        org = ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2)
        render(Page::Organization::CompoundShowComponent.new(org, {}))
      end
    end
  end
end
