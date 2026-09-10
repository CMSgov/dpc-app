# frozen_string_literal: true

module Page
  module Utility
    # Shows Access denied reason
    class AccessDeniedComponent < ViewComponent::Base
      def initialize(failure_code:, status_display: nil, organization: nil, role: nil)
        super()
        @organization = organization
        @failure_code = failure_code
        @role = role
        @icon, @classes, @status = status_display
      end
    end
  end
end
