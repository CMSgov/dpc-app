# frozen_string_literal: true

module Core
  module Form
    # Renders <input type="text"...
    class TextAreaComponent < ViewComponent::Base
      attr_accessor :label, :attribute, :default, :input_options, :hint

      # rubocop:disable Metrics/ParameterLists
      def initialize(label:, attribute:, default: '', input_options: {}, hint: '', error_msg: '')
        super
        @label = label
        @attribute = attribute
        @input_options = input_options
        @hint = hint
        @error_msg = error_msg
        @default = default
        input_options[:class] ||= []
        input_options[:class] << 'usa-textarea'
        @input_options[:class] << 'usa-input--error' if error_msg.present?
      end
      # rubocop:enable Metrics/ParameterLists
    end
  end
end
