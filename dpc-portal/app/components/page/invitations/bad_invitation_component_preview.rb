# frozen_string_literal: true

module Page
  module Invitations
    # Displays unfixable error message in accept invitation process
    class BadInvitationComponentPreview < ViewComponent::Preview
      def invalid_invitation
        reason = 'invalid'
        render(Page::Invitations::BadInvitationComponent.new(reason, 'warning'))
      end

      def pii_mismatch
        reason = 'pii_mismatch'
        render(Page::Invitations::BadInvitationComponent.new(reason, 'error'))
      end

      # @param error_code
      def verification_failure(error_code: :user_not_authorized_official)
        render(Page::Invitations::BadInvitationComponent.new(error_code, 'error'))
      end
    end
  end
end
