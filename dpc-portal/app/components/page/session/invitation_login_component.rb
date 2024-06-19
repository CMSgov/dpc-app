# frozen_string_literal: true

module Page
  module Session
    # Component for Invitatation login (IAL/2 flow)
    class InvitationLoginComponent < ViewComponent::Base
      def initialize(invitation)
        super
        @invitation = invitation
      end
    end
  end
end
