# frozen_string_literal: true

module Page
  module Invitations
    # Displays accept invitation form
    class AcceptInvitationComponent < ViewComponent::Base
      def initialize(organization, invitation, given_name, family_name)
        super
        @organization = organization
        @invitation = invitation
        @name = "#{given_name} #{family_name}"
      end
    end
  end
end
