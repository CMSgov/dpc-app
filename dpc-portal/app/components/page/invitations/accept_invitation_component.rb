# frozen_string_literal: true

module Page
  module Invitations
    # Displays accept invitation form
    class AcceptInvitationComponent < ViewComponent::Base
      def initialize(organization, invitation)
        super
        @organization = organization
        @invitation = invitation
      end
    end
  end
end
