# frozen_string_literal: true

module Core
  module Button
    # Button
    # ----------------
    #
    # [See at USWDS](https://designsystem.digital.gov/components/button/)
    #
    class ButtonComponentPreview < ViewComponent::Preview
      # @param label "Label of the button"
      # @param destination "Destination when button is clicked"
      def default(label: 'Text', destination: root_path)
        render(Core::Button::ButtonComponent.new(label:, destination:))
      end
    end
  end
end
