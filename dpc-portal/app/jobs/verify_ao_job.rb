# frozen_string_literal: true

# A background job that verifies AOs are still valid
# It uses two environment variables to control number of records and how far to look back:
# VERIFICATION_MAX_RECORDS: limits the number of records check to manage CPI API GW throttling
# VERIFICATION_LOOKBACK_HOURS: defines how long a record can be valid without needing to be checked
class VerifyAoJob < ApplicationJob
  queue_as :portal

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
    link_error_attributes = { last_checked_at: Time.now, verification_status: false,
                              verification_reason: message }
    other_error_attributes = link_error_attributes.merge(verification_status: 'rejected')
    AoOrgLink.transaction do
      link.update!(link_error_attributes)
      case message
      when 'ao_med_sanctions'
        link.user.update!(other_error_attributes)
        link.provider_organization.update!(other_error_attributes)
        unverify_all_links_and_orgs(link.user, message)
      when 'no_approved_enrollment'
        link.provider_organization.update!(other_error_attributes)
      end
    end
  end

  def links_to_check
    max_records = ENV.fetch('VERIFICATION_MAX_RECORDS', '10').to_i
    lookback_hours = ENV.fetch('VERIFICATION_LOOKBACK_HOURS', '144').to_i
    AoOrgLink.where(last_checked_at: ..lookback_hours.hours.ago,
                    verification_status: true).limit(max_records)
  end

  def unverify_all_links_and_orgs(user, message)
    link_error_attributes = { last_checked_at: Time.now, verification_status: false,
                              verification_reason: message }
    other_error_attributes = link_error_attributes.merge(verification_status: 'rejected')
    AoOrgLink.where(user:, verification_status: true).each do |link|
      link.update!(link_error_attributes)
      link.provider_organization.update!(other_error_attributes)
    end
  end
end
