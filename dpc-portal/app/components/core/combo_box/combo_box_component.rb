# frozen_string_literal: true

module Core
  module ComboBox
    # Render a USWDS-styled combo box.
    class ComboBoxComponent < ViewComponent::Base
        def initialize(label:, id:, options:)
            super
            @label = label
            @id = id
            @options = options
        end
    end
  end
end
