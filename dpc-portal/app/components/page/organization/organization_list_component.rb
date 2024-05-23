# frozen_string_literal: true

module Page
  module Organization
    # Render a USWDS-styled organization list for an implementer.
    class OrganizationListComponent < ViewComponent::Base
      def initialize(ao_or_cd:, organizations:)
        super
        @ao = ao_or_cd == :ao
        @organizations = organizations
        @org_names = organizations.map(&:name)
        @org_npis = organizations.map(&:npi)
      end

      def ao?
        !!@ao
      end
    end
  end
end
