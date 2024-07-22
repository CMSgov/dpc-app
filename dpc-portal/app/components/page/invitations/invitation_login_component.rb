# frozen_string_literal: true

module Page
  module Invitations
    # Component for Invitatation login (IAL/2 flow)
    class InvitationLoginComponent < ViewComponent::Base
      def initialize(invitation)
        super
        @invitation = invitation
        @musts = [
          'Email address (you must use the same email that received the invitation)',
          'Drivers license or state ID',
          'Social security number',
          'Phone number'
        ]
      end
    end
  end
end
