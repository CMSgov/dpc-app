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
      def multiple_options(label: 'Label', id: 'id')
        render(Core::ComboBox::ComboBoxComponent.new(label: label, id: id, options: ['one', 'two', 'three']))
      end

      # @param label "Label of the combo box"
      # @param id "ID of the combo box"
      def one_option(label: 'Label', id: 'id')
        render(Core::ComboBox::ComboBoxComponent.new(label: label, id: id, options: ['only option']))
      end

      # @param label "Label of the combo box"
      # @param id "ID of the combo box"
      def no_options(label: 'Label', id: 'id')
        render(Core::ComboBox::ComboBoxComponent.new(label: label, id: id, options: []))
      end
    end
  end
end
