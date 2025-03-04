# frozen_string_literal: true

# Shared functions of Verify Jobs
module Verification
  extend ActiveSupport::Concern

  included do
    def max_records
      ENV.fetch('VERIFICATION_MAX_RECORDS', '10').to_i
    end

    def lookback_hours
      ENV.fetch('VERIFICATION_LOOKBACK_HOURS', '144').to_i
    end

    def link_error_attributes(message)
      { last_checked_at: Time.now, verification_status: false,
        verification_reason: message,
        audit_comment: LoggingConstants::ActionContext::BatchVerificationCheck }
    end

    def entity_error_attributes(message)
      link_error_attributes(message).merge(verification_status: 'rejected')
    end

    def update_org_sanctions(org, message)
      org.update!(entity_error_attributes(message))
      org.ao_org_links.where(verification_status: true).each do |link|
        link.update!(link_error_attributes(message))
        log_error(link, message)
      end
    end

    def log_error(link, message)
      logger.info(["#{self.class.name} Check Fail",
                   { actionContext: LoggingConstants::ActionContext::BatchVerificationCheck,
                     actionType: LoggingConstants::ActionType::FailCpiApiGwCheck,
                     verificationReason: message,
                     authorizedOfficial: link.user.id,
                     providerOrganization: link.provider_organization.id }])
    end

    def enqueue_job(klass)
      klass.perform_later
    end

    def log_batch_verification_waivers(role_and_waivers)
      if role_and_waivers[:has_org_waiver]
        logger.info(['Organization has a waiver',
                     { actionContext: LoggingConstants::ActionContext::BatchVerificationCheck,
                       actionType: LoggingConstants::ActionType::OrgHasWaiver }])
      end
      return unless role_and_waivers[:has_ao_waiver]

      logger.info(['Authorized official has a waiver',
                   { actionContext: LoggingConstants::ActionContext::BatchVerificationCheck,
                     actionType: LoggingConstants::ActionType::AoHasWaiver }])
    end
  end
end
