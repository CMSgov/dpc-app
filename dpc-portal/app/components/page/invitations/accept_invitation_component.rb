# frozen_string_literal: true

module Page
  module Invitations
    # Displays accept invitation form
    class AcceptInvitationComponent < ViewComponent::Base
      def initialize(organization, invitation, given_name, family_name)
        super
        @organization = organization
        @invitation = invitation
        @musts = [
          'Your role as the Authorized Official',
          'Your organization\'s Medicare enrollment status',
          'That you or your organization are not on the Medicare exclusion list'
        ]
      end
    end
  end
end
