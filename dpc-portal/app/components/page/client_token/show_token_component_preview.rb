# frozen_string_literal: true

module Page
  module ClientToken
    # renders a preview of the show client token
    class ShowTokenComponentPreview < ViewComponent::Preview
      def default
        org = ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2)
        client_token = { 'label' => 'Prod Token', 'token' => SecureRandom.base64(34) }
        render(Page::ClientToken::ShowTokenComponent.new(org, client_token))
      end
    end
  end
end
