# frozen_string_literal: true

# A background job that verifies AOs are still valid
# It uses two environment variables to control number of records and how far to look back:
# VERIFICATION_MAX_RECORDS: limits the number of records to check to manage CPI API GW throttling
# VERIFICATION_LOOKBACK_HOURS: defines how long a record can be valid without needing to be checked
class VerifyAoJob < ApplicationJob
  queue_as :portal
  include Verification

  def perform
    @start = Time.now
    service = AoVerificationService.new
    links_to_check.each do |link|
      config_attributes(link)
      check_link(service, link)
      update_success(link)
    rescue AoException => e
      handle_error(link, e.message)
    end
    if links_to_check.present?
      enqueue_job(VerifyAoJob)
    else
      enqueue_job(VerifyProviderOrganizationJob)
    end
  end

  def check_link(service, link)
    response = service.check_ao_eligibility(link.provider_organization.npi, :pac_id, link.user.pac_id)
    log_batch_verification_waivers(response)
  end

  def update_success(link)
    AoOrgLink.transaction do
      link.update!(last_checked_at: Time.now)
      link.provider_organization.update!(last_checked_at: Time.now)
      link.user.update!(last_checked_at: Time.now)
    end
  end

  def handle_error(link, message)
    AoOrgLink.transaction do
      link.update!(link_error_attributes(message))
      case message
      when 'org_med_sanctions'
        update_org_sanctions(link.provider_organization, message)
      when 'ao_med_sanctions'
        update_ao_sanctions(link, message)
      when 'user_not_authorized_official'
        link.provider_organization.update!(last_checked_at: Time.now)
      when 'no_approved_enrollment'
        link.provider_organization.update!(entity_error_attributes(message))
      end
    end
    log_error(link, message)
  end

  def update_ao_sanctions(link, message)
    link.user.update!(entity_error_attributes(message))
#     link.provider_organization.update!(entity_error_attributes(message))
    unverify_all_links_and_orgs(link.user, message)
  end

  def links_to_check
    AoOrgLink.where(last_checked_at: ..lookback_hours.hours.ago,
                    verification_status: true).limit(max_records)
  end

  def unverify_all_links_and_orgs(user, message)
    AoOrgLink.where(user:, verification_status: true).each do |link|
      link.update!(link_error_attributes(message))
#       link.provider_organization.update!(entity_error_attributes(message))
      logger.info(["#{self.class.name} Check Fail",
                   { actionContext: LoggingConstants::ActionContext::BatchVerificationCheck,
                     actionType: LoggingConstants::ActionType::FailCpiApiGwCheck,
                     verificationReason: message,
                     authorizedOfficial: link.user.id,
                     providerOrganization: link.provider_organization.id }])
    end
  end

  private

  def config_attributes(link)
    CurrentAttributes.save_organization_attributes(link.provider_organization, link.user)
    CurrentAttributes.save_user_attributes(link.user)
  end
end
