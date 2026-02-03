# frozen_string_literal: true

module Page
  module Invitations
    # Component for Invitatation login (IAL/2 flow)
    class InvitationLoginComponent < ViewComponent::Base
      def initialize(invitation)
        super
        @invitation = invitation
        @musts = [
          'The email address your invite was sent to',
          'Driver\'s license or state-issued ID',
          'Social Security number',
          'Phone number',
          'A phone or camera to take and upload photos'
        ]
      end
    end
  end
end
