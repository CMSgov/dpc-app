# frozen_string_literal: true

module Core
  module Icon
    # Renders an icon available from USWDS
    class UswdsComponent < ViewComponent::Base
      def initialize(name, size: 2, additional_classes: [])
        super()
        @name = name
        @size = size
        @additional_classes = additional_classes
      end

      def icon_classes
        classes = @additional_classes
        # uswds only 'usa-icon--*' classes only start at size-3 (and end at size-9)
        classes << ['usa-icon', (@size >= 3 && @size <= 9) ? "usa-icon--size-#{@size}" : '']
        classes.uniq.join(' ').strip
      end
    end
  end
end
