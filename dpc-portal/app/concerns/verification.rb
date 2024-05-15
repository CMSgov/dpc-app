# frozen_string_literal: true

# Shared functions of Verify Jobs
module Verification
  extend ActiveSupport::Concern

  included do
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
      end
    end
  end
end
