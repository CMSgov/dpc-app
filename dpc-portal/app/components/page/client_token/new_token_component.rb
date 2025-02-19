# frozen_string_literal: true

module Page
  module ClientToken
    # Renders client_tokens/new
    class NewTokenComponent < ViewComponent::Base
      attr_accessor :organization, :obj_name

      def initialize(organization, errors: {})
        super
        @organization = organization
        @obj_name = 'client token'
        @errors = errors
      end
    end
  end
end
