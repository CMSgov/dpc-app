# frozen_string_literal: true

module Page
  module Organization
    # Shows the Organization#view
    class CredentialsComponent < ViewComponent::Base
      attr_accessor :organization

      def initialize(organization)
        super
        @organization = organization
      end
    end
  end
end
