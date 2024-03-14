# frozen_string_literal: true

module Core
  module Card
    # Organization Card Component
    # ----------------
    #
    # [See at USWDS](https://designsystem.digital.gov/components/card/)
    #
    class OrganizationCardComponentPreview < ViewComponent::Preview

      # @after_render :wrap_in_ul
      def default
        org = ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2)
        render(Core::Card::OrganizationCardComponent.new(organization: org))
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
