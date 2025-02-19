# frozen_string_literal: true

module Page
  module Utility
    # Shows Access denied reason
    class AccessDeniedComponent < ViewComponent::Base
      def initialize(failure_code:, organization: nil)
        super
        @organization = organization
        @failure_code = failure_code
      end
    end
  end
end
