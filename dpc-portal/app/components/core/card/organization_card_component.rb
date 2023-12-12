# frozen_string_literal: true

module Core
  module Card
    # Render a USWDS-styled card for an organization.
    class OrganizationCardComponent < ViewComponent::Base
      def initialize(name:, npi:, status:, status_color:)
        super
        @name = name
        @npi = npi
        @status = status
        @status_color = status_color
      end
    end
  end
end
