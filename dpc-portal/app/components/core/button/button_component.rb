# frozen_string_literal: true

module Core
  module Button
    # Render a USWDS-styled button.
    class ButtonComponent < ViewComponent::Base
      def initialize(label:, on_click:)
        super
        @label = label
        @on_click = on_click
      end
    end
  end
end
