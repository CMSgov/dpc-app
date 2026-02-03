# frozen_string_literal: true

module Page
  module Invitations
    # Displays unfixable error message in accept invitation process
    class CdFlowFailComponentPreview < ViewComponent::Preview
      # @param error_code select :error_codes
      # @param step
      def verification_failure(error_code: :pii_mismatch, step: 2)
        user = User.new(given_name: 'Robert', family_name: 'Hodges')
        invitation = Invitation.new(id: 4, invited_by: user, invitation_type: :credential_delegate)
        render(Page::Invitations::CdFlowFailComponent.new(invitation, error_code, step))
      end

      private

      def error_codes
        { choices: %i[pii_mismatch missing_info server_error fail_to_proof] }
      end
    end
  end
end
