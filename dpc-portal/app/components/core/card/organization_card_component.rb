# frozen_string_literal: true

module Core
  module Card
    # Render a USWDS-styled card for an organization.
    class OrganizationCardComponent < ViewComponent::Base
      with_collection_parameter :link
      def initialize(link:)
        super
        @link = link
        @organization = link.provider_organization
      end

      def before_render
        message_prefix = @link.is_a?(AoOrgLink) ? 'verification' : 'cd_access'
        @icon, @classes, @status = if @organization.rejected?
                                     ['lock', %i[text-gray-50],
                                      t("#{message_prefix}.#{@organization.verification_reason}_status")]
                                   elsif !@link.verification_status?
                                     ['lock', %i[text-gray-50],
                                      t("verification.#{@link.verification_reason}_status")]
                                   elsif @organization.terms_of_service_accepted_at.blank?
                                     ['warning', %i[text-gold], t("#{message_prefix}.tos_not_signed")]
                                   else
                                     ['verified', %i[text-accent-cool], t("#{message_prefix}.manage_org")]
                                   end
      end
    end
  end
end
