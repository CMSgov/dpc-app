# frozen_string_literal: true

module Page
  module CredentialDelegate
    # Previews Invite Credential Delegate form
    class NewInvitationComponentPreview < ViewComponent::Preview
      def new
        render(Page::CredentialDelegate::NewInvitationComponent.new(MockOrg.new('Health Hut'), CdInvitation.new))
      end

      def filled_in
        cd_invite = CdInvitation.new(given_name: 'Bob',
                                     family_name: 'Hogan',
                                     phone_raw: '877-288-3131',
                                     email: 'bob@example.com',
                                     email_confirmation: 'bob@example.com')
        render(Page::CredentialDelegate::NewInvitationComponent.new(MockOrg.new('Health Hut'), cd_invite))
      end

      def errors
        cd_invite = CdInvitation.new
        cd_invite.valid?
        render(Page::CredentialDelegate::NewInvitationComponent.new(MockOrg.new('Health Hut'), cd_invite))
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
