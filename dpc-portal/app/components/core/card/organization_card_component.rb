# frozen_string_literal: true

module Core
  module Card
    # Render a USWDS-styled card for an organization.
    class OrganizationCardComponent < ViewComponent::Base
      include Organization
      with_collection_parameter :link

      def initialize(link:)
        super
        @link = link
        @organization = link.provider_organization
      end

      def before_render
        status_display = user_status(@organization, @link)
        @icon = status_display[:icon]
        @classes = status_display[:classes]
        @status = status_display[:status]
      end
    end
  end
end
