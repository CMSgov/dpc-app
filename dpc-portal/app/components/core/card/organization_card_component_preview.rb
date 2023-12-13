# frozen_string_literal: true

require 'ostruct'

module Core
  module Card
    # Organization Card Component
    # ----------------
    #
    # [See at USWDS](https://designsystem.digital.gov/components/card/)
    #
    class OrganizationCardComponentPreview < ViewComponent::Preview
      def default()
        testOrg = OpenStruct.new(name: "Test Organization", npi: "npi_123456")
        render(Core::Card::OrganizationCardComponent.new(organization: testOrg))
      end
    end
  end
end
