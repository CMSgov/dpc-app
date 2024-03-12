# frozen_string_literal: true

module Core
  module Form
    # Preview of text input component
    class TextInputComponentPreview < ViewComponent::Preview
      #
      # @param label textarea
      # @param hint textarea
      # @param error textarea
      def parameterized(label: 'Some label', hint: 'Here is a hint', error: '')
        render(Core::Form::TextInputComponent.new(label:, attribute: :foo, hint:, error_msg: error))
      end
    end
  end
end
