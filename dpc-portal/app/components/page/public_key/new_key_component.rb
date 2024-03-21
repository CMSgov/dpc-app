# frozen_string_literal: true

module Page
  module PublicKey
    # Renders public_keys/new
    class NewKeyComponent < ViewComponent::Base
      attr_accessor :organization, :obj_name

      def initialize(organization)
        super
        @organization = organization
        @obj_name = 'public key'
      end
    end
  end
end
