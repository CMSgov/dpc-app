# frozen_string_literal: true

module Page
  module ClientToken
    # Renders client_tokens/new
    class NewTokenComponent < ViewComponent::Base
      attr_accessor :organization

      def initialize(organization)
        super
        @organization = organization
      end
    end
  end
end
