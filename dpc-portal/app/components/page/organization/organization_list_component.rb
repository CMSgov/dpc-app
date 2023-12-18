# frozen_string_literal: true

module Page
  module Organization
    # Render a USWDS-styled organization list for an implementer.
    class OrganizationListComponent < ViewComponent::Base
      def initialize(organizations:)
        super
        @organizations = organizations
      end
    end
  end
end
