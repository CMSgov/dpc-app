# frozen_string_literal: true

module Core
  module Table
    # Render a USWDS-styled table.
    class TableComponent < ViewComponent::Base
      attr_accessor :id, :sortable

      def initialize(id: nil, additional_classes: nil, sortable: false)
        super
        @id = id
        @additional_classes = additional_classes
        @sortable = sortable
      end

      def table_classes
        classes = @additional_classes || []
        classes << 'usa-table'
        classes.uniq.join(' ')
      end
    end
  end
end
