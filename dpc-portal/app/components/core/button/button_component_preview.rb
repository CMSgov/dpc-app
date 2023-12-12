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
      # @param on_click "Action to take when button is clicked"
      def default(label: 'Text', on_click: "alert('hi');")
        render(Core::Button::ButtonComponent.new(label: label, on_click: on_click))
      end
    end
  end
end
