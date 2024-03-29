# frozen_string_literal: true

module Core
  module Form
    # Renders <input type="text"...
    class TextAreaComponent < ViewComponent::Base
      attr_accessor :label, :attribute, :default, :input_options, :hint

      def initialize(label:, attribute:, default: '', input_options: {}, hint: '')
        super
        @label = label
        @attribute = attribute
        @input_options = input_options
        @hint = hint
        @default = default
        input_options[:class] ||= []
        input_options[:class] << 'usa-textarea'
      end
    end
  end
end
