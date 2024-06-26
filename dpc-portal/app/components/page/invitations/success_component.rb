# frozen_string_literal: true

module Page
  module Invitations
    # Displays successful registration message
    class SuccessComponent < ViewComponent::Base
      def initialize(organization, invitation)
        super
        @organization = organization
        @invitation = invitation
      end
    end
  end
end
