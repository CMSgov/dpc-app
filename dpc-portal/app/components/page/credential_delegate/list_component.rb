# frozen_string_literal: true

module Page
  module CredentialDelegate
    # Lists the Credential Delegates
    class ListComponent < ViewComponent::Base
      attr_reader :organization, :active_credential_delegates, :pending_credential_delegates

      def initialize(organization, pending_invitations, expired_invitations, credential_delegates)
        super
        @organization = organization
        @active_credential_delegates = credential_delegates.map(&:show_attributes)
        @pending_credential_delegates = pending_invitations.map(&:show_attributes)
        @expired_invitations = expired_invitations.map(&:show_attributes)
      end
    end
  end
end
