# frozen_string_literal: true

module Core
  module Card
    # Organization Card Component
    # ----------------
    #
    # [See at USWDS](https://designsystem.digital.gov/components/card/)
    #
    class OrganizationCardComponentPreview < ViewComponent::Preview
      # @param name "Name of the organization"
      # @param npi "NPI of the organization"
      def default(name: 'Org Name', npi: 'npi_123_abc')
        render(Core::Card::OrganizationCardComponent.new(name: name, npi: npi))
      end
    end
  end
end
