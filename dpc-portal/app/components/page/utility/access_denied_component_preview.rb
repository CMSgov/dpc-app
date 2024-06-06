# frozen_string_literal: true

module Page
  module Utility
    # Previews Access Denied Page
    class AccessDeniedComponentPreview < ViewComponent::Preview
      # @param failure_code
      def with_org(failure_code: 'verification.org_med_sanctions')
        org = ProviderOrganization.new(id: 5, name: 'Test Organization', npi: 'npi_123456',
                                       terms_of_service_accepted_at: 2.days.ago)
        render(Page::Utility::AccessDeniedComponent.new(organization: org, failure_code:))
      end

      # @param failure_code
      def no_org(failure_code: 'verification.ao_med_sanctions')
        render(Page::Utility::AccessDeniedComponent.new(failure_code:))
      end
    end
  end
end
