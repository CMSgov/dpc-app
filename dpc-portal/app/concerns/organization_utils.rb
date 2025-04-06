# frozen_string_literal: true

# Shared Organization helper functions
module OrganizationUtils
  extend ActiveSupport::Concern

  included do
    def org_status(organization, link)
      message_prefix = link.is_a?(AoOrgLink) ? 'verification' : 'cd_access'
#       organization_config_complete = organization.check_config_complete
      organization_config_complete = organization.public_ips.present? && organization.public_keys.present? && organization.client_tokens.present?
      if organization.terms_of_service_accepted_at.blank?
        ['warning', %i[text-gold], t("#{message_prefix}.sign_tos")]
      elsif organization.rejected?
        ['lock', %i[text-gray-50], t("#{message_prefix}.access_denied")]
      elsif link.user.can_access?(organization)
        ['link_off', %i[text-gray-50], t("#{message_prefix}.api_disabled")]
      elsif !organization_config_complete
        ['warning', %i[text-gold], t("#{message_prefix}.configuration_needed")]
      else
        ['check', %i[text-green], t("#{message_prefix}.configuration_complete")]
      end
    end
  end
end
