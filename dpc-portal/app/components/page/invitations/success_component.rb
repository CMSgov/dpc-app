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
        @musts = [
          'Sign Terms of Service',
          'Complete API setup',
          'Optional: Invite a Credential Delegate to setup and manage your organization\'s API access'
        ]
      end
    end
  end
end
