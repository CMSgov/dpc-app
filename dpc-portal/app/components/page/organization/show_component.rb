# frozen_string_literal: true

module Page
  module Organization
    # Shows the Organization#view
    class ShowComponent < ViewComponent::Base
      attr_accessor :organization

      def initialize(organization)
        @organization = organization
        super
      end
    end
  end
end
