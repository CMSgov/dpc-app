# frozen_string_literal: true

module Core
  module Icon
    # Previews an icon available from USWDS
    class UswdsComponentPreview < ViewComponent::Preview
      # @param icon_name
      # @param size
      # @param icon_classes
      def parameterized(icon_name: 'lock', size: 1, icon_classes: nil)
        additional_classes = (icon_classes || '').split
        render(Core::Icon::UswdsComponent.new(icon_name, size:, additional_classes:))
      end
    end
  end
end
