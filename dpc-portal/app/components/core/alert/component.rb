# frozen_string_literal: true

module Core
  module Alert
    # Render a USWDS-styled alert.
    class Component < ViewComponent::Base
      attr_accessor :status, :include_icon, :heading

      VALID_STATUSES = %i[info warning error success notice alert].freeze

      def initialize(status: '', heading: '', include_icon: true)
        super

        @valid = VALID_STATUSES.include?(status.to_sym) || status.blank?
        log_error(status) unless @valid

        @status = case status
                  when '', 'notice', :notice
                    :info
                  when 'alert', :alert
                    :error
                  else
                    status
                  end
        @include_icon = include_icon
        @heading = heading
      end

      def log_error(status)
        Rails.logger.error(["Unexpected Flash Status: #{status}",
                            { actionContext: LoggingConstants::ActionContext::Rendering,
                              actionType: LoggingConstants::ActionType::InvalidFlashStatus }])
      end
    end
  end
end
