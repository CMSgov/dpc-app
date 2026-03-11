# frozen_string_literal: true

module Core
  module Icon
    # Renders an icon available from USWDS
    class UswdsComponent < ViewComponent::Base
      def initialize(name, size: nil, additional_classes: [])
        super()
        @name = name
        @size = size
        @additional_classes = additional_classes
      end

      def icon_classes
        # uswds only 'usa-icon--*' classes only start at size-3 (and end at size-9)
        size_class = @size&.between?(3, 9) ? "usa-icon--size-#{@size}" : nil
        (@additional_classes + ['usa-icon', size_class]).compact.uniq.join(' ')
      end
    end
  end
end
