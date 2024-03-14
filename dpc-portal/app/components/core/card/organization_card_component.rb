# frozen_string_literal: true

module Core
  module Card
    # Render a USWDS-styled card for an organization.
    class OrganizationCardComponent < ViewComponent::Base
      with_collection_parameter :organization
      def initialize(organization:)
        super
        @organization = organization
      end
    end
  end
end
