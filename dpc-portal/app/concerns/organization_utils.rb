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
        ['warning', %i[text-gray-50], t("#{message_prefix}.sign_tos")]
      elsif organization.rejected?
        ['lock', %i[text-gray-50], t("#{message_prefix}.access_denied")]
      elsif !organization_config_complete
        ['warning', %i[text-gray-50], t("#{message_prefix}.configuration_needed")]
      else
        ['check', %i[text-gray-50], t("#{message_prefix}.configuration_complete")]
      end

#       elsif API_DISABLED
#         ['link_off', %i[text-gray-50], t("#{message_prefix}.api_disabled")]
# 1.)
#     sign_tos: Sign terms of service
# <svg class="usa-icon" aria-hidden="true" focusable="false" role="img">
#           <use href="/assets/img/sprite.svg#warning"></use>
#         </svg>
# 2.)
#     configuration_needed: Configuration needed
# <svg class="usa-icon" aria-hidden="true" focusable="false" role="img">
#           <use href="/assets/img/sprite.svg#warning"></use>
#         </svg>
# 3.)
#     configuration_complete: Configuration complete
# <svg class="usa-icon" aria-hidden="true" focusable="false" role="img">
#         <use href="/assets/img/sprite.svg#check"></use>
#       </svg>
#
# 4.)
#     api_disabled: API disabled
# <svg class="usa-icon" aria-hidden="true" focusable="false" role="img">
#         <use href="/assets/img/sprite.svg#link_off"></use>
#       </svg>
# 5.)
#     access_denied: Access denied
# <svg class="usa-icon" aria-hidden="true" focusable="false" role="img">
#         <use href="/assets/img/sprite.svg#lock"></use>
#       </svg>
    end
  end
end
