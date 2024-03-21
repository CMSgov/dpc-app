# frozen_string_literal: true

module Page
  module CredentialDelegate
    # Previews Invite Credential Delegate form
    class NewInvitationComponentPreview < ViewComponent::Preview
      def new
        render(Page::CredentialDelegate::NewInvitationComponent.new(org, Invitation.new))
      end

      def filled_in
        cd_invite = Invitation.new(invited_given_name: 'Bob',
                                   invited_family_name: 'Hogan',
                                   phone_raw: '877-288-3131',
                                   invited_email: 'bob@example.com',
                                   invited_email_confirmation: 'bob@example.com')
        render(Page::CredentialDelegate::NewInvitationComponent.new(org, cd_invite))
      end

      def errors
        cd_invite = Invitation.new
        cd_invite.valid?
        render(Page::CredentialDelegate::NewInvitationComponent.new(org, cd_invite))
      end

      private

      def org
        ProviderOrganization.new(name: 'Health Hut', npi: '1111111111', id: 2)
      end
    end
  end
end
