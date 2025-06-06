# frozen_string_literal: true

module Page
  module Organization
    # Shows tabbed credential delegates and credentials
    class CompoundShowComponentPreview < ViewComponent::Preview
      def authorized_official
        org = ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2)
        status_display = ['verified', %i[text-accent-cool], 'Manage your organization.']
        render(Page::Organization::CompoundShowComponent.new(org, { active: [], pending: [], expired: [] }, true,
                                                             'Authorized Official', status_display))
      end

      def credential_delegate
        org = ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2)
        status_display = ['lock', %i[text-gray-50], 'Your organization is in the Medicare Exclusions Database.']
        render(Page::Organization::CompoundShowComponent.new(org, {}, true, 'Credential Delegate', status_display))
      end
    end
  end
end
