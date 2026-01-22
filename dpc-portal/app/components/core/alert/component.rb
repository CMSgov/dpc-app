# frozen_string_literal: true

module Core
  module Alert
    # Render a USWDS-styled alert.
    class Component < ViewComponent::Base
      attr_accessor :status, :include_icon, :heading

      def initialize(status: '', heading: '', include_icon: true)
        super

        if %i[info warning error success notice alert].include?(status.to_sym) || status.blank?
          @valid = true
        else
          log_error(status)
          @valid = false
        end

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
