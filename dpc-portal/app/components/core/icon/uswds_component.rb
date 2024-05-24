# frozen_string_literal: true

module Core
  module Icon
    # Renders an icon available fror USWDS
    class UswdsComponent < ViewComponent::Base
      def initialize(name, size: 1, additional_classes: [])
        super()
        @name = name
        @size = size
        @additional_classes = additional_classes
      end

      def icon_classes
        classes = @additional_classes
        classes << 'usa-icon'
        classes.uniq.join(' ')
      end
    end
  end
end
