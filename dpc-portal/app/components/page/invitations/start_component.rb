# frozen_string_literal: true

module Page
  module Invitations
    # First page the user sees when accepting a valid invitation
    class StartComponent < ViewComponent::Base
      def initialize(organization, invitation)
        super
        @organization = organization
        @invitation = invitation
        @hours, @minutes = invitation.expires_in
      end
    end
  end
end
