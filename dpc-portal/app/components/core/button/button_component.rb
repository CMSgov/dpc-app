# frozen_string_literal: true

module Core
  module Button
    # Render a USWDS-styled button.
    class ButtonComponent < ViewComponent::Base
      attr_accessor :label, :destination, :additional_class
      def initialize(label:, destination:, additional_class: nil)
        super
        @label = label
        @destination = destination
        @additional_class = additional_class
      end
    end
  end
end
