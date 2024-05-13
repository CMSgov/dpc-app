# frozen_string_literal: true

# A background job that verifies Provider Organizations are still valid
class VerifyProviderOrganizationJob < ApplicationJob
  queue_as :portal

  def perform
    service = AoVerificationService.new
    orgs_to_check.each do |org|
      service.check_org_med_sanctions(org.npi)
      service.get_approved_enrollments(org.npi)
      org.update!(last_checked_at: Time.now)
    rescue AoException => e
      handle_error(org, e.message)
    end
    puts "Gateway calls: #{service.cpi_api_gw_client.counter}"
  end

  def handle_error(org, message)
    org_error_attributes = { last_checked_at: Time.now, verification_status: 'rejected',
                             verification_reason: message }
    org.update!(org_error_attributes)
    link_error_attributes = org_error_attributes.merge(verification_status: false)
    org.ao_org_links.where(verification_status: true).each do |link|
      link.update!(link_error_attributes)
    end
  end

  def orgs_to_check
    max_records = ENV.fetch('MAX_RECORDS', '10').to_i
    lookback_hours = ENV.fetch('LOOKBACK_HOURS', '144').to_i
    ProviderOrganization.where(last_checked_at: ..lookback_hours.hours.ago,
                               verification_status: 'approved').limit(max_records)
  end
end
