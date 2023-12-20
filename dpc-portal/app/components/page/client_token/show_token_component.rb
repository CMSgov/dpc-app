# frozen_string_literal: true

module Page
  module ClientToken
    # Renders token info after create
    class ShowTokenComponent < ViewComponent::Base
      attr_accessor :organization, :client_token

      def initialize(organization, client_token)
        super
        @organization = organization
        @client_token = client_token
      end
    end
  end
end
