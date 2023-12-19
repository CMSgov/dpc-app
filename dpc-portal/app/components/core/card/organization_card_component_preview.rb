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

      def default
        render(Core::Card::OrganizationCardComponent.new(organization: OrgStruct.new('Test Organization', 'npi_123456',
                                                                                     'api_123')))
      end
    end
  end
end
