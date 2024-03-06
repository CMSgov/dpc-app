# frozen_string_literal: true

module Core
  module Form
    # Renders masked input fields
    class MaskedInputComponent < ViewComponent::Base
      attr_accessor :label, :attribute, :default, :input_options, :hint, :error_msg

      # rubocop:disable Metrics/ParameterLists
      def initialize(label:, attribute:, mask:, default: '', hint: '', error_msg: '')
        super
        @label = label
        @attribute = attribute
        @input_options = { class: %w[usa-input usa-masked] }
        @hint = hint
        @error_msg = error_msg
        @default = default
        @input_options[:class] << 'usa-input--error' if error_msg.present?
        case mask
        when 'us-phone'
          @input_options[:placeholder] = '___-___-____'
          @input_options[:pattern] = '\d{3}-\d{3}-\d{4}'
          @input_options['aria-describedby'] = 'telHint'
        end
      end
      # rubocop:enable Metrics/ParameterLists
    end
  end
end
