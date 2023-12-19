# frozen_string_literal: true

module Core
  module Form
    class TextInputComponent < ViewComponent::Base
      attr_accessor :label, :attribute, :default, :input_options, :hint
      def initialize(label:, attribute:, default: '', input_options: {}, hint: '')
        @label = label
        @attribute = attribute
        @input_options = input_options
        @hint = hint
        @default = default
        input_options[:class] ||= []
        input_options[:class] << 'usa-input'
      end
    end
  end
end
