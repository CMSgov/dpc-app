# frozen_string_literal: true

module Page
  module Invitations
    # Displays unfixable error message in accept invitation process
    class AoFlowFailComponentPreview < ViewComponent::Preview
      # @param error_code select :error_codes
      # @param step
      def verification_failure(error_code: :user_not_authorized_official, step: 1)
        invitation = Invitation.new(id: 3, provider_organization: ProviderOrganization.new(id: 1, name: 'Health Hut'))
        render(Page::Invitations::AoFlowFailComponent.new(invitation, error_code, step))
      end

      private

      def error_codes
        { choices: %i[user_not_authorized_official no_approved_enrollment bad_npi org_med_sanctions ao_med_sanctions
                      missing_info server_error] }
      end
    end
  end
end
