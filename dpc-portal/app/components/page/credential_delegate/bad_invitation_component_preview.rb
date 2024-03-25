# frozen_string_literal: true

module Page
  module CredentialDelegate
    # Displays unfixable error message in accept invitation process
    class BadInvitationComponentPreview < ViewComponent::Preview
      def invalid_invitation
        reason = 'invalid'
        render(Page::CredentialDelegate::BadInvitationComponent.new(reason))
      end

      def pii_mismatch
        reason = 'pii_mismatch'
        render(Page::CredentialDelegate::BadInvitationComponent.new(reason))
      end
    end
  end
end
