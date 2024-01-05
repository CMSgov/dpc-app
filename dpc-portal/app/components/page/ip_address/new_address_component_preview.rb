# frozen_string_literal: true

module Page
  module IpAddress
    # Renders ip_address/new preview
    class NewAddressComponentPreview < ViewComponent::Base
      def default
        render(Page::IpAddress::NewAddressComponent.new(MockOrg.new('Health Hut')))
      end
    end

    # Mocks dpc-api organization
    class MockOrg
      attr_accessor :name, :path_id

      def initialize(name)
        @name = name
        @path_id = 'some-guid'
      end
    end
  end
end
