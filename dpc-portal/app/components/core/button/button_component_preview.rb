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
      def default(label: 'Text', destination: root_path, additional_class: nil)
        render(Core::Button::ButtonComponent.new(label: label,
                                                 destination: destination,
                                                 additional_class: additional_class))
      end

      def outline_button(label: 'Text', destination: root_path, additional_class: 'usa-button--outline')
        render(Core::Button::ButtonComponent.new(label: label,
                                                 destination: destination,
                                                 additional_class: additional_class))
      end
    end
  end
end
