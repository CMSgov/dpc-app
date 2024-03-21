# frozen_string_literal: true

module Core
  module ComboBox
    # ComboBox
    # ----------------
    #
    # [See at USWDS](https://designsystem.digital.gov/components/combo-box/)
    #
    class ComboBoxComponentPreview < ViewComponent::Preview
      # @param label "Label of the combo box"
      # @param id "ID of the combo box"
      # @param on_change "Function to call when combo box selection is changed"
      def multiple_options(label: 'Label', id: 'id', on_change: "alert('changed');")
        render(Core::ComboBox::ComboBoxComponent.new(label:, id:, options: %w[one two three],
                                                     on_change:))
      end

      # @param label "Label of the combo box"
      # @param id "ID of the combo box"
      # @param on_change "Function to call when combo box selection is changed"
      def one_option(label: 'Label', id: 'id', on_change: "alert('changed');")
        render(Core::ComboBox::ComboBoxComponent.new(label:, id:, options: ['only option'],
                                                     on_change:))
      end

      # @param label "Label of the combo box"
      # @param id "ID of the combo box"
      # @param on_change "Function to call when combo box selection is changed"
      def no_options(label: 'Label', id: 'id', on_change: "alert('changed');")
        render(Core::ComboBox::ComboBoxComponent.new(label:, id:, options: [], on_change:))
      end
    end
  end
end
