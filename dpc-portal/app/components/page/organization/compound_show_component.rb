# frozen_string_literal: true

module Page
  module Organization
    # Shows tabbed credential delegates and credentials
    class CompoundShowComponent < ViewComponent::Base
      def initialize(organization, delegate_information, credential_start, role, invitation)
        super
        @links = [['User Access', '#credential_delegates', !credential_start],
                  ['Credentials', '#credentials', credential_start]]
        @organization = organization
        @active_credential_delegates = delegate_information[:active]
        @pending_credential_delegates = delegate_information[:pending]
        @expired_cd_invitations = delegate_information[:expired]
        @show_cds = !delegate_information.empty?
        @role = role
        if invitation.status.match?('accepted') then
          @show_status_alert = false
        else
          @show_status_alert = true
        end
        @status = invitation.status.capitalize
      end
    end
  end
end
