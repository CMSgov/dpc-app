# frozen_string_literal: true

module Page
  module CredentialDelegate
    # Render a USWDS-styled invite-cd form for a controller.
    class NewInvitationComponent < ViewComponent::Base
      attr_reader :organization, :cd_invite, :phone_input_options

      def initialize(organization, cd_invite)
        super
        @organization = organization
        @cd_invite = cd_invite
        @phone_input_options = { maxlength: 12, placeholder: '___-___-____' }
      end
    end
  end
end
