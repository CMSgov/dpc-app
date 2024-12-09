# frozen_string_literal: true

module Page
  module Invitations
    # Previews successful registration message
    class SuccessComponentPreview < ViewComponent::Preview
      def success_cd
        org = ProviderOrganization.new(id: 2, name: 'Health Hut', npi: '1111111111')
        user = User.new(given_name: 'Robert', family_name: 'Hodges')
        invitation = Invitation.new(id: 4, invited_by: user, invitation_type: :credential_delegate)
        render(Page::Invitations::SuccessComponent.new(org, invitation, 'Paola', 'Pineiro'))
      end

      def success_ao
        org = ProviderOrganization.new(id: 2, name: 'Health Hut', npi: '1111111111')
        user = User.new(given_name: 'Robert', family_name: 'Hodges')
        invitation = Invitation.new(id: 4, invited_by: user, invitation_type: :authorized_official)
        render(Page::Invitations::SuccessComponent.new(org, invitation, 'Paola', 'Pineiro'))
      end
    end
  end
end
