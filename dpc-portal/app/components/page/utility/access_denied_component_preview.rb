# frozen_string_literal: true

module Page
  module Utility
    # Previews Access Denied Page
    class AccessDeniedComponentPreview < ViewComponent::Preview
      # rubocop:disable Layout/LineLength
      # @param failure_code select { choices: [verification.org_med_sanctions, verification.user_not_authorized_official, verification.ao_med_sanctions, verification.no_approved_enrollment] }
      # rubocop:enable Layout/LineLength
      def with_org(failure_code: 'verification.org_med_sanctions')
        org = ProviderOrganization.new(id: 5, name: 'Test Organization', npi: 'npi_123456',
                                       terms_of_service_accepted_at: 2.days.ago,
                                       dpc_api_organization_id: 'id_123456789')
        status_display = ['lock', %i[text-gray-50], 'Access denied']
        render(Page::Utility::AccessDeniedComponent.new(failure_code:, status_display:, organization: org,
                                                        role: 'Authorized Official'))
      end

      # @param failure_code
      def no_org(failure_code: 'verification.ao_med_sanctions')
        render(Page::Utility::AccessDeniedComponent.new(failure_code:))
      end
    end
  end
end
