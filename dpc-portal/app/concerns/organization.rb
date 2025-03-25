# frozen_string_literal: true

# Shared Organization helper functions
module Organization
  extend ActiveSupport::Concern

  included do
    def user_status(organization, link)
      message_prefix = link.is_a?(AoOrgLink) ? 'verification' : 'cd_access'
      if organization.rejected?
        { icon: 'lock', classes: %i[text-gray-50],
          status: t("#{message_prefix}.#{organization.verification_reason}_status") }
      elsif !link.verification_status?
        { icon: 'lock', classes: %i[text-gray-50], status: t("verification.#{link.verification_reason}_status") }
      elsif organization.terms_of_service_accepted_at.blank?
        { icon: 'warning', classes: %i[text-gold], status: t("#{message_prefix}.tos_not_signed") }
      else
        { icon: 'verified', classes: %i[text-accent-cool], status: t("#{message_prefix}.manage_org") }
      end
    end
  end
end
