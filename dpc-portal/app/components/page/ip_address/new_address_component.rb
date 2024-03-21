# frozen_string_literal: true

module Page
  module IpAddress
    # Renders ip_address/new
    class NewAddressComponent < ViewComponent::Base
      attr_accessor :organization, :obj_name

      def initialize(organization)
        super
        @organization = organization
        @obj_name = 'IP address'
      end
    end
  end
end
