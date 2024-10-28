# frozen_string_literal: true

module Page
  module CredentialDelegate
    # Render a USWDS-styled invite-cd form for a controller.
    class NewInvitationComponent < ViewComponent::Base
      attr_reader :organization, :cd_invite, :errors

      def initialize(organization, cd_invite, errors: {})
        super
        @organization = organization
        @cd_invite = cd_invite
        @errors = errors
      end
    end
  end
end
