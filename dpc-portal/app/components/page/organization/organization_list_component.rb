# frozen_string_literal: true

module Page
  module Organization
    # Render a USWDS-styled organization list for an implementer.
    class OrganizationListComponent < ViewComponent::Base
      def initialize(ao_or_cd:, links:)
        super
        @ao = ao_or_cd == :ao
        @links = links
      end

      def ao?
        @ao
      end
    end
  end
end
