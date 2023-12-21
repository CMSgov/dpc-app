# frozen_string_literal: true

module Page
  module PublicKey
    # Renders public_keys/new
    class NewKeyComponent < ViewComponent::Base
      attr_accessor :organization

      def initialize(organization)
        super
        @organization = organization
      end
    end
  end
end
