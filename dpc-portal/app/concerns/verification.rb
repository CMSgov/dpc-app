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
        verification_reason: message }
    end

    def entity_error_attributes(message)
      link_error_attributes(message).merge(verification_status: 'rejected')
    end

    def update_org_sanctions(org, message)
      org.update!(entity_error_attributes(message))
      org.ao_org_links.where(verification_status: true).each do |link|
        link.update!(link_error_attributes(message))
        logger.info(['AO Check Fail',
                     { actionContext: LoggingConstants::ActionContext::AoVerificationCheck,
                       verificationReason: message,
                       authorizedOfficial: link.user.id,
                       providerOrganization: org.id }])
      end
    end

    def enqueue_job(klass)
      klass.perform_later
    end
  end
end
