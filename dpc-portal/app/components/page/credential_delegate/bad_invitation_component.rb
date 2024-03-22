# frozen_string_literal: true

module Page
  module CredentialDelegate
    # Displays unfixable error message in accept invitation process
    class BadInvitationComponent < ViewComponent::Base
      def initialize(reason)
        super
        @reason = reason
      end
    end
  end
end
