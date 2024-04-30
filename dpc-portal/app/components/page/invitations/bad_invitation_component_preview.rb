# frozen_string_literal: true

module Page
  module Invitations
    # Displays unfixable error message in accept invitation process
    class BadInvitationComponentPreview < ViewComponent::Preview
      def invalid_invitation
        reason = 'invalid'
        render(Page::Invitations::BadInvitationComponent.new(reason))
      end

      def pii_mismatch
        reason = 'pii_mismatch'
        render(Page::Invitations::BadInvitationComponent.new(reason))
      end
    end
  end
end
