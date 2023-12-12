# frozen_string_literal: true

module Core
  module Button
    # Button
    # ----------------
    #
    # [See at USWDS](https://designsystem.digital.gov/components/button/)
    #
    class ButtonComponentPreview < ViewComponent::Preview
      # @param text "Text of the button"
      # @param on_click "onClick action to take when button is pressed"
      def default(text: 'Text', on_click: '')
        render(Core::Button::ButtonComponent.new(text: text, on_click: on_click))
      end
    end
  end
end
