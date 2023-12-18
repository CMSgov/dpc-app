# frozen_string_literal: true

module Core
  module Card
    # Render a USWDS-styled alert.
    class BasicComponent < ViewComponent::Base
      attr_accessor :text_content, :button_params

      def initialize(text_content: '<h1>Welcome</h1>', button_params: nil)
        super
        @text_content = text_content
        @button_params = button_params
      end
    end
  end
end
