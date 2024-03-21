# frozen_string_literal: true

module Core
  module Card
    # Organization Card Component
    # ----------------
    #
    # [See at USWDS](https://designsystem.digital.gov/components/card/)
    #
    class OrganizationCardComponentPreview < ViewComponent::Preview
      OrgStruct = Struct.new(:name, :npi, :api_id)

      # @after_render :wrap_in_ul
      def default
        render(Core::Card::OrganizationCardComponent.new(organization: OrgStruct.new('Test Organization', 'npi_123456',
                                                                                     'api_123')))
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
