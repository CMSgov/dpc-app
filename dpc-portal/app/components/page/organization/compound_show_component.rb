# frozen_string_literal: true

module Page
  module Organization
    # Shows tabbed credential delegates and credentials
    class CompoundShowComponent < ViewComponent::Base
      def initialize(organization, cd_invitations, expired_cd_invitations, credential_delegates, show_cds)
        super
        @links = [['User Access', '#credential_delegates', true],
                  ['Credentials', '#credentials', false]]
        @organization = organization
        @active_credential_delegates = credential_delegates
        @pending_credential_delegates = cd_invitations
        @expired_cd_invitations = expired_cd_invitations
        @show_cds = show_cds
      end
    end
  end
end
