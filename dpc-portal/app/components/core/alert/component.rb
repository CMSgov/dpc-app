# frozen_string_literal: true

module Core
  module Alert
    # Render a USWDS-styled alert.
    class Component < ViewComponent::Base
      attr_accessor :status, :include_icon, :heading

      def initialize(status: '', heading: '', include_icon: true)
        super

        @status = case status
                  when '', :notice
                    :info
                  when :alert
                    :error
                  else
                    status
                  end
        @include_icon = include_icon
        @heading = heading
      end
    end
  end
end
