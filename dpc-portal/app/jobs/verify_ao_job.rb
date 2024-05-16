# frozen_string_literal: true

# A background job that verifies AOs are still valid
# It uses two environment variables to control number of records and how far to look back:
# VERIFICATION_MAX_RECORDS: limits the number of records to check to manage CPI API GW throttling
# VERIFICATION_LOOKBACK_HOURS: defines how long a record can be valid without needing to be checked
class VerifyAoJob < ApplicationJob
  queue_as :portal
  include Verification

  def perform
    service = AoVerificationService.new
    links_to_check.each do |link|
      service.check_ao_eligibility(link.provider_organization.npi, :pac_id, link.user.pac_id)
      AoOrgLink.transaction do
        link.update!(last_checked_at: Time.now)
        link.user.update!(last_checked_at: Time.now)
      end
    rescue AoException => e
      handle_error(link, e.message)
    end
  end

  def handle_error(link, message)
    AoOrgLink.transaction do
      link.update!(link_error_attributes(message))
      case message
      when 'ao_med_sanctions'
        link.user.update!(entity_error_attributes(message))
        link.provider_organization.update!(entity_error_attributes(message))
        unverify_all_links_and_orgs(link.user, message)
      when 'no_approved_enrollment'
        link.provider_organization.update!(entity_error_attributes(message))
      end
    end
  end

  def links_to_check
    AoOrgLink.where(last_checked_at: ..lookback_hours.hours.ago,
                    verification_status: true).limit(max_records)
  end

  def unverify_all_links_and_orgs(user, message)
    AoOrgLink.where(user:, verification_status: true).each do |link|
      link.update!(link_error_attributes(message))
      link.provider_organization.update!(entity_error_attributes(message))
    end
  end
end
