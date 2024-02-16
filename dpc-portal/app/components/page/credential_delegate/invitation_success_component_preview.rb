# frozen_string_literal: true

module Page
  module CredentialDelegate
    # Previews Invite Credential Delegate Success Page
    class InvitationSuccessComponentPreview < ViewComponent::Preview
      def default
        render(Page::CredentialDelegate::InvitationSuccessComponent.new(MockOrg.new('Health Hut')))
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
