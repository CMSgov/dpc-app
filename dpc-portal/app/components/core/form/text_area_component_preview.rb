# frozen_string_literal: true

module Core
  module Form
    # Preview of text input component
    class TextAreaComponentPreview < ViewComponent::Preview
      #
      # @param label textarea
      # @param hint textarea
      # @param default textarea
      def parameterized(label: 'Some label', hint: 'Here is a hint', default: '')
        input_options = { rows: 9, readonly: 'readonly' }
        render(Core::Form::TextAreaComponent.new(label:, attribute: :foo, default:, hint:,
                                                 input_options:))
      end
    end
  end
end
