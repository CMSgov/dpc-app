# frozen_string_literal: true

module Page
  module PublicKey
    # Renders public_keys/new preview
    class NewKeyComponentPreview < ViewComponent::Preview
      def default
        render(Page::PublicKey::NewKeyComponent.new(MockOrg.new('Health Hut')))
      end
    end

    # Mocks dpc-api organization
    class MockOrg
      attr_accessor :name, :id

      def initialize(name)
        @name = name
        @id = 'some-guid'
      end
    end
  end
end
