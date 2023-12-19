# frozen_string_literal: true

module Core
  module Form
    class TextInputComponentPreview < ViewComponent::Preview
      #
      # @param label textarea
      # @param hint textarea
      def parameterized(label: 'Some label', hint: 'Here is a hint')
        render(Core::Form::TextInputComponent.new(label: label, attribute: :foo, hint: hint))
      end
    end
  end
end
