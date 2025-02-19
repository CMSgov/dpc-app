# frozen_string_literal: true

module Page
  module Invitations
    # Displays successful registration message
    class SuccessComponent < ViewComponent::Base
      def initialize(organization, invitation, given_name, family_name)
        super
        @organization = organization
        @invitation = invitation
        @name = "#{given_name} #{family_name}"
      end
    end
  end
end
