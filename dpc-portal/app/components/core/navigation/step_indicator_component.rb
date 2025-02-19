# frozen_string_literal: true

module Core
  module Navigation
    # Step Indicator Component
    # ----------------
    #
    # [Based on USWDS](https://designsystem.digital.gov/components/step-indicator/)
    #
    class StepIndicatorComponent < ViewComponent::Base
      def initialize(steps, index)
        super
        @steps = steps
        @index = index
      end
    end
  end
end
