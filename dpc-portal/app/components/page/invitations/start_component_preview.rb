# frozen_string_literal: true

module Page
  module Invitations
    # First page the user sees when accepting a valid invitation
    class StartComponentPreview < ViewComponent::Preview
      def start_ao
        org = ProviderOrganization.new(id: 2, name: 'Health Hut', npi: '3664337706')
        invitation = Invitation.new(id: 4, invited_email: 'doug@example.com', invitation_type: :authorized_official,
                                    created_at: 47.hours.ago - 59.minutes)
        render(Page::Invitations::StartComponent.new(org, invitation))
      end

      def start_cd
        org = ProviderOrganization.new(id: 2, name: 'Health Hut', npi: '3664337706')
        inviter = User.new(given_name: 'Bob', family_name: 'Hodges')
        invitation = Invitation.new(id: 4, invited_email: 'paula@example.com', invitation_type: :credential_delegate,
                                    invited_given_name: 'Paula', invited_family_name: 'Pineiro', invited_by: inviter,
                                    created_at: 47.hours.ago - 59.minutes)
        render(Page::Invitations::StartComponent.new(org, invitation))
      end
    end
  end
end
