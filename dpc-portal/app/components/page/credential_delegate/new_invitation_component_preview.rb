# frozen_string_literal: true

module Page
  module CredentialDelegate
    # Previews Invite Credential Delegate form
    class NewInvitationComponentPreview < ViewComponent::Preview
      def new
        render(Page::CredentialDelegate::NewInvitationComponent.new(MockOrg.new('Health Hut'), Invitation.new))
      end

      def filled_in
        cd_invite = Invitation.new(invited_given_name: 'Bob',
                                   invited_family_name: 'Hogan',
                                   phone_raw: '877-288-3131',
                                   invited_email: 'bob@example.com',
                                   invited_email_confirmation: 'bob@example.com')
        render(Page::CredentialDelegate::NewInvitationComponent.new(MockOrg.new('Health Hut'), cd_invite))
      end

      def errors
        cd_invite = Invitation.new
        cd_invite.valid?
        render(Page::CredentialDelegate::NewInvitationComponent.new(MockOrg.new('Health Hut'), cd_invite))
      end
    end

    # Mocks dpc-api organization
    class MockOrg
      attr_accessor :name, :npi, :path_id

      def initialize(name)
        @name = name
        @npi = '11111111'
        @path_id = SecureRandom.uuid
      end
    end
  end
end
