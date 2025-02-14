# frozen_string_literal: true

module Core
  module Navigation
    # Step Indicator Component
    # ----------------
    #
    # [Based on USWDS](https://designsystem.digital.gov/components/step-indicator/)
    #
    class StepIndicatorComponentPreview < ViewComponent::Preview
      #
      # @param steps
      # @param index
      def parameterized(steps: 'Step One,Step Two,Step Three', index: '1')
        steps_a = steps.split(',')
        index_i = index.to_i
        render(Core::Navigation::StepIndicatorComponent.new(steps_a, index_i))
      end
    end
  end
end
