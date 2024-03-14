# frozen_string_literal: true

module Page
  module IpAddress
    # Renders ip_address/new preview
    class NewAddressComponentPreview < ViewComponent::Preview
      def default
        org = ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2)
        render(Page::IpAddress::NewAddressComponent.new(org))
      end
    end
  end
end
