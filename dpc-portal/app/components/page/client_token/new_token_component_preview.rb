# frozen_string_literal: true

module Page
  module ClientToken
    # Renders client_tokens/new preview
    class NewTokenComponentPreview < ViewComponent::Preview
      def default
        render(Page::ClientToken::NewTokenComponent.new(MockOrg.new('Health Hut')))
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
