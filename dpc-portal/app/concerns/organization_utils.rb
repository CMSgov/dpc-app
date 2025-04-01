# frozen_string_literal: true

# Shared Organization helper functions
module OrganizationUtils
  extend ActiveSupport::Concern

  included do
    def org_status(organization, link)
      message_prefix = link.is_a?(AoOrgLink) ? 'verification' : 'cd_access'
      if organization.rejected?
        ['lock', %i[text-gray-50], t("#{message_prefix}.#{organization.verification_reason}_status")]
      elsif !link.verification_status?
        ['lock', %i[text-gray-50], t("verification.#{link.verification_reason}_status")]
      elsif organization.terms_of_service_accepted_at.blank?
        ['warning', %i[text-gold], t("#{message_prefix}.tos_not_signed")]
      else
        ['verified', %i[text-accent-cool], t("#{message_prefix}.manage_org")]
      end
    end
  end
end
