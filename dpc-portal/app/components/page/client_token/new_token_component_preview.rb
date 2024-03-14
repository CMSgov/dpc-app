# frozen_string_literal: true

module Page
  module ClientToken
    # Renders client_tokens/new preview
    class NewTokenComponentPreview < ViewComponent::Preview
      def default
        render(Page::ClientToken::NewTokenComponent.new(ProviderOrganization.new(name: 'Health Hut', npi: '1111111111',
                                                                                 id: 2)))
      end
    end
  end
end
