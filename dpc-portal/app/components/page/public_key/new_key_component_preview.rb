# frozen_string_literal: true

module Page
  module PublicKey
    # Renders public_keys/new preview
    class NewKeyComponentPreview < ViewComponent::Preview
      # @param show_errors "Show errors" toggle
      def default(show_errors: false)
        errors = if show_errors
                   { label: 'Cannot be blank',
                     public_key: 'Cannot be blank',
                     snippet_signature: "Can't be blank" }
                 else
                   {}
                 end
        org = ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2)
        render(Page::PublicKey::NewKeyComponent.new(org, errors:))
      end
    end
  end
end
