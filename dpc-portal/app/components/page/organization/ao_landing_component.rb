# frozen_string_literal: true

module Page
  module Organization
    # Render a USWDS-styled landing page with organization list for an implementer.
    class AoLandingComponent < ViewComponent::Base
      def initialize(organizations:)
        super
        @organizations = organizations
      end
    end
  end
end
