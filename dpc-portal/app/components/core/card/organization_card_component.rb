# frozen_string_literal: true

module Core
  module Card
    # Render a USWDS-styled card for an organization.
    class OrganizationCardComponent < ViewComponent::Base
      with_collection_parameter :organization
      def initialize(organization:)
        super
        @organization = organization
      end

      def before_render
        @icon, @classes, @status = if @organization.rejected?
                                     ['lock', %i[text-gray-50],
                                      t("verification.#{@organization.verification_reason}_status")]
                                   elsif @organization.terms_of_service_accepted_at.blank?
                                     ['warning', %i[text-gold], 'You must sign DPC Terms of Service.']
                                   else
                                     ['verified', %i[text-accent-cool], 'Manage your organization.']
                                   end
      end
    end
  end
end
