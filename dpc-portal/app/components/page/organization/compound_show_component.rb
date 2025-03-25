# frozen_string_literal: true

module Page
  module Organization
    # Shows tabbed credential delegates and credentials
    class CompoundShowComponent < ViewComponent::Base
      def initialize(organization, delegate_information, credential_start, role, status_display)
        super
        @links = [['Credential Delegates', '#credential_delegates', !credential_start],
                  ['API Configuration', '#credentials', credential_start]]
        @organization = organization
        @active_credential_delegates = delegate_information[:active]
        @pending_credential_delegates = delegate_information[:pending]
        @expired_cd_invitations = delegate_information[:expired]
        @show_cds = !delegate_information.empty?
        @role = role
        @icon = status_display[:icon]
        @classes = status_display[:classes]
        @status = status_display[:status]
      end
    end
  end
end
