# frozen_string_literal: true

module Page
  module Organization
    # Shows tabbed credential delegates and credentials
    class CompoundShowComponent < ViewComponent::Base
      def initialize(organization, delegate_information)
        super
        @links = [['User Access', '#credential_delegates', true],
                  ['Credentials', '#credentials', false]]
        @organization = organization
        @active_credential_delegates = delegate_information[:active]
        @pending_credential_delegates = delegate_information[:pending]
        @expired_cd_invitations = delegate_information[:expired]
        @show_cds = !delegate_information.empty?
      end
    end
  end
end
