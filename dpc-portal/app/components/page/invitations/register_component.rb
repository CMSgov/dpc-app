# frozen_string_literal: true

module Page
  module Invitations
    # Displays register invitation form
    class RegisterComponent < ViewComponent::Base
      def initialize(organization, invitation)
        super
        @organization = organization
        @invitation = invitation
      end
    end
  end
end
