# frozen_string_literal: true

module Core
  module Card
    # Render a USWDS-styled card for an organization.
    class OrganizationCardComponent < ViewComponent::Base
      include OrganizationUtils
      with_collection_parameter :link

      def initialize(link:)
        super
        @link = link
        @organization = link.provider_organization
      end

      def before_render
        message_prefix = @link.is_a?(AoOrgLink) ? 'verification' : 'cd_access'
        @icon, @classes, @status = org_status(@organization, @link)
      end
    end
  end
end
