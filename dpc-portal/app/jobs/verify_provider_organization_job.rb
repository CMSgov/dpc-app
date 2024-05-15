# frozen_string_literal: true

# A background job that verifies Provider Organizations are still valid
# It uses two environment variables to control number of records and how far to look back:
# VERIFICATION_MAX_RECORDS: limits the number of records to check to manage CPI API GW throttling
# VERIFICATION_LOOKBACK_HOURS: defines how long a record can be valid without needing to be checked
class VerifyProviderOrganizationJob < ApplicationJob
  queue_as :portal
  include Verification

  def perform
    service = AoVerificationService.new
    unless orgs_to_check.each do |org|
             service.check_org_med_sanctions(org.npi)
             service.get_approved_enrollments(org.npi)
             org.update!(last_checked_at: Time.now)
    rescue AoException => e
      handle_error(org, e.message)
           end.empty?
      VerifyProviderOrganizationJob.perform_later
    end
  end

  def handle_error(org, message)
    ProviderOrganization.transaction do
      org.update!(entity_error_attributes(message))
      update_org_sanctions(org, message)
    end
  end

  def orgs_to_check
    max_records = ENV.fetch('VERIFICATION_MAX_RECORDS', '10').to_i
    lookback_hours = ENV.fetch('VERIFICATION_LOOKBACK_HOURS', '144').to_i
    ProviderOrganization.where(last_checked_at: ..lookback_hours.hours.ago,
                               verification_status: 'approved').limit(max_records)
  end
end
