# frozen_string_literal: true

module Core
  module Table
    # Render a USWDS-styled table.
    class HeaderComponent < ViewComponent::Base
      Column = Struct.new(
        :label,
        :sortable,
        keyword_init: true
      )
      attr_reader :caption, :columns, :sorts

      def initialize(caption: '', columns: [])
        super
        @caption = caption
        @columns = columns.map do |col|
          Column.new(
            label: col[:label] || '',
            sortable: col[:sortable] || false
          )
        end
      end
    end
  end
end
