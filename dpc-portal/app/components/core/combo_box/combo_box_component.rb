# frozen_string_literal: true

module Core
  module ComboBox
    # Render a USWDS-styled combo box.
    class ComboBoxComponent < ViewComponent::Base
      def initialize(label:, id:, options:, on_change:)
        super
        @label = label
        @id = id
        @options = options
        @on_change = on_change
      end
    end
  end
end
