# frozen_string_literal: true

module Core
  module Card
    # Organization Card Component
    # ----------------
    #
    # [See at USWDS](https://designsystem.digital.gov/components/card/)
    #
    class OrganizationCardComponentPreview < ViewComponent::Preview
      # @param tos_accepted
      # @param failure_code
      # @after_render :wrap_in_ul
      def parameterized(failure_code: nil, tos_accepted: 'true')
        if ProviderOrganization.defined_enums['verification_reason'].include? failure_code
          verification_status = 'rejected'
          verification_reason = failure_code
        else
          verification_status = verification_reason = nil
        end

        terms_of_service_accepted_at = tos_accepted == 'true' ? 2.days.ago : nil
        org = ProviderOrganization.new(id: 5, name: 'Test Organization', npi: 'npi_123456',
                                       terms_of_service_accepted_at:, verification_reason:, verification_status:)
        link = AoOrgLink.new(provider_organization: org)
        render(Core::Card::OrganizationCardComponent.new(link:))
      end

      private

      def wrap_in_ul(html, _)
        <<~HTML
          <ul class="usa-card-group">
              #{html}
          </ul>
        HTML
      end
    end
  end
end
