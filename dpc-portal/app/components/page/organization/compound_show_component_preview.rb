# frozen_string_literal: true

module Page
  module Organization
    # Shows tabbed credential delegates and credentials
    class CompoundShowComponentPreview < ViewComponent::Preview
      def authorized_official
        org = ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2, dpc_api_organization_id: '09106579-d3bd-49d4-bd40-03b3ae5e142d')
        render(Page::Organization::CompoundShowComponent.new(org, { active: [], pending: [], expired: [] }, true, 'Authorized Official', :accepted))
      end

      def credential_delegate
        org = ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2, dpc_api_organization_id: '09106579-d3bd-49d4-bd40-03b3ae5e142d')
        render(Page::Organization::CompoundShowComponent.new(org, {}, true, 'Credential Delegate', :expired))
      end
    end
  end
end
