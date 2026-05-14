# frozen_string_literal: true

module Core
  module Table
    # Render a USWDS-styled table.
    class HeaderComponent < ViewComponent::Base
      Column = Struct.new(
        :label,
        :sortable
      )
      attr_reader :caption, :columns

      def initialize(caption: '', columns: [])
        super()
        @caption = caption
        @columns = columns.map do |col|
          Column.new(
            label: col.fetch(:label, ''),
            sortable: col[:sortable]
          )
        end
      end
    end
  end
end
