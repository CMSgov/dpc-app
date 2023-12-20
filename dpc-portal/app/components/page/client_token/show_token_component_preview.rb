# frozen_string_literal: true

module Page
  module ClientToken
    # renders a preview of the show client token
    class ShowTokenComponentPreview < ViewComponent::Preview
      def default
        client_token = { 'label' => 'Prod Token', 'token' => SecureRandom.base64(34) }
        render(Page::ClientToken::ShowTokenComponent.new(MockOrg.new('Health Hut'), client_token))
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
