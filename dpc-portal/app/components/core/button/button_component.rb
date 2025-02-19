# frozen_string_literal: true

module Core
  module Button
    # Render a USWDS-styled button.
    class ButtonComponent < ViewComponent::Base
      attr_accessor :label, :destination, :method, :additional_classes

      def initialize(label:, destination:, method: :get, additional_classes: nil, disabled: false)
        super
        @label = label
        @destination = destination
        @method = method
        @disabled = disabled
        @additional_classes = additional_classes
      end

      def button_classes
        classes = @additional_classes || []
        classes << 'usa-button'
        classes.uniq.join(' ')
      end
    end
  end
end
