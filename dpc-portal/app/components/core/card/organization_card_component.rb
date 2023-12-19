# frozen_string_literal: true

module Core
  module Card
    # Render a USWDS-styled card for an organization.
    class OrganizationCardComponent < ViewComponent::Base
      def initialize(organization:)
        super
        @name = organization.name
        @npi = organization.npi
        @api_id = organization.api_id
      end
    end
  end
end
