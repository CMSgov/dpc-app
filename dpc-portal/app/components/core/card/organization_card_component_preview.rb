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
        render(Core::Card::OrganizationCardComponent.new(organization: OpenStruct.new(name: "Test Organization", npi: "npi_123456")))
      end
    end
  end
end
