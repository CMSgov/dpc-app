# frozen_string_literal: true

module Page
  module IpAddress
    # Renders ip_address/new preview
    class NewAddressComponentPreview < ViewComponent::Preview
      # @param show_errors "Show errors" toggle
      def default(show_errors: false)
        errors = if show_errors
                   { label: 'Cannot be blank',
                     ip_address: "Can't be blank" }
                 else
                   {}
                 end
        org = ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2)
        render(Page::IpAddress::NewAddressComponent.new(org, errors:))
      end
    end
  end
end
