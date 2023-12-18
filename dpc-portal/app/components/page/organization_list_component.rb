# frozen_string_literal: true

module Page
  module Page
    # Render a USWDS-styled organization list for an implementer.
    class OrganizationListComponent < ViewComponent::Base
      def initialize(organizations:)
        super
        @organizations = organizations
      end
    end
  end
end
