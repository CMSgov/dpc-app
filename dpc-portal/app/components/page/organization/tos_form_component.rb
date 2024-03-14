# frozen_string_literal: true

module Page
  module Organization
    # Form to accept Terms of Service
    class TosFormComponent < ViewComponent::Base
      attr_reader :organization

      def initialize(organization)
        super
        @organization = organization
      end
    end
  end
end
