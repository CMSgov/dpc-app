# frozen_string_literal: true

module Page
  module Organization
    # Render a USWDS-styled new organization form for a controller.
    class NewOrganizationComponent < ViewComponent::Base
      attr_reader :npi_error

      def initialize(npi_error)
        super
        @npi_error = npi_error
      end
    end
  end
end
