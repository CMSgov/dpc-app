# frozen_string_literal: true

module Page
  module PublicKey
    # Renders public_keys/new preview
    class NewKeyComponentPreview < ViewComponent::Preview
      def default
        org = ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2)
        render(Page::PublicKey::NewKeyComponent.new(org))
      end
    end
  end
end
