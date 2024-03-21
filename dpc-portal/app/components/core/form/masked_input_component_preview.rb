# frozen_string_literal: true

module Core
  module Form
    # Preview of masked input component
    # https://designsystem.digital.gov/components/input-mask/
    class MaskedInputComponentPreview < ViewComponent::Preview
      # @after_render :wrap_in_form
      #
      # @param label textarea
      # @param hint textarea
      # @param error textarea
      def parameterized(label: 'Some label', hint: 'Here is a hint', error: '')
        render(Core::Form::MaskedInputComponent.new(label:, attribute: :foo, mask: 'us-phone', hint:,
                                                    error_msg: error))
      end

      private

      def wrap_in_form(html, _context)
        <<~HTML
           <form>
            #{html}
          </form>
        HTML
      end
    end
  end
end
