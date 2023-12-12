# frozen_string_literal: true

module Core
  module Card
    # Render a USWDS-styled card for an organization.
    class OrganizationCardComponent < ViewComponent::Base
      def initialize(name:, npi:)
        super
        @name = name
        @npi = npi
      end
    end
  end
end
