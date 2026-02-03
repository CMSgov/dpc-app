# frozen_string_literal: true

module Page
  module Invitations
    # Displays unfixable error message in accept invitation process
    class CdFlowFailComponent < ViewComponent::Base
      def initialize(invitation, reason, step)
        super
        @invitation = invitation
        @step = step.to_i
        @org_name = invitation&.provider_organization&.name
        @reason = reason.to_sym
        @status = "verification.#{@reason}_status"
        @text = "verification.#{@reason}_text"
        @ao_email = "foo@bar.baz"
      end
    end
  end
end
