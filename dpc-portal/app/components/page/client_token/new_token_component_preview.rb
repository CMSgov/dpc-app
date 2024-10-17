# frozen_string_literal: true

module Page
  module ClientToken
    # Renders client_tokens/new preview
    class NewTokenComponentPreview < ViewComponent::Preview
      # @param show_errors "Show errors" toggle
      def default(show_errors: false)
        errors = show_errors ? { label: 'Cannot be blank' } : {}
        org = ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2)
        render(Page::ClientToken::NewTokenComponent.new(org, errors:))
      end
    end
  end
end
