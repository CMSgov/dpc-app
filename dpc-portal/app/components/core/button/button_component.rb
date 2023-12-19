# frozen_string_literal: true

module Core
  module Button
    # Render a USWDS-styled button.
    class ButtonComponent < ViewComponent::Base
      def initialize(label:, destination:)
        super
        @label = label
        @destination = destination
      end
    end
  end
end
