# frozen_string_literal: true

module Page
  module Invitations
    # Displays Otp form
    class OtpComponentPreview < ViewComponent::Preview
      def default
        org = ProviderOrganization.new(id: 2, name: 'Health Hut')
        user = User.new(given_name: 'Robert', family_name: 'Hodges')
        invitation = Invitation.new(id: 4, invited_by: user, invitation_type: :credential_delegate)
        render(Page::Invitations::OtpComponent.new(org, invitation))
      end
    end
  end
end
