# frozen_string_literal: true

module Page
  module CredentialDelegate
    # Displays accept invitation form
    class AcceptInvitationComponent < ViewComponent::Base
      def initialize(organization, cd_invite)
        super
        @organization = organization
        @cd_invite = cd_invite
      end
    end
  end
end
