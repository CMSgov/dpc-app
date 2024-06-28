# frozen_string_literal: true

module Page
  module Invitations
    # Previews successful registration message
    class SuccessComponentPreview < ViewComponent::Preview
      def success_cd
        org = ProviderOrganization.new(id: 2, name: 'Health Hut')
        user = User.new(given_name: 'Robert', family_name: 'Hodges')
        invitation = Invitation.new(id: 4, invited_by: user, invitation_type: :credential_delegate)
        render(Page::Invitations::SuccessComponent.new(org, invitation))
      end

      def success_ao
        org = ProviderOrganization.new(id: 2, name: 'Health Hut')
        user = User.new(given_name: 'Robert', family_name: 'Hodges')
        invitation = Invitation.new(id: 4, invited_by: user, invitation_type: :authorized_official)
        render(Page::Invitations::SuccessComponent.new(org, invitation))
      end
    end
  end
end
