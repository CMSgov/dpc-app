# frozen_string_literal: true

module Page
  module Organization
    # Page shown after successful completion of adding new organization.
    class NewOrganizationSuccessComponent < ViewComponent::Base
      def initialize(organization)
        super
        @organization = organization
      end
    end
  end
end
