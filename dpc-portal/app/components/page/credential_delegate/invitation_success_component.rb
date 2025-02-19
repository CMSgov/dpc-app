# frozen_string_literal: true

module Page
  module CredentialDelegate
    # Page shown after successful completion of invitation form.
    class InvitationSuccessComponent < ViewComponent::Base
      attr_reader :organization

      def initialize(organization)
        super
        @organization = organization
      end
    end
  end
end
