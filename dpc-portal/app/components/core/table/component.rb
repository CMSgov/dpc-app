# frozen_string_literal: true

module Core
  module Table
    # Render a USWDS-styled table.
    class Component < ViewComponent::Base
      attr_accessor :id, :caption, :header_rows, :body_rows, :striped,
                    :borderless, :stacked, :stacked_header

      def initialize(id: nil, striped: false, borderless: false, stacked: false,
                     stacked_header: false)
        super
        @id = id
        @striped = striped
        @borderless = borderless
        @stacked = stacked
        @stacked_header = stacked_header
      end

      def table_classes
        classes = ['usa-table']
        classes << 'usa-table--striped' if striped
        classes << 'usa-table--borderless' if borderless
        classes << 'usa-table--stacked' if stacked
        classes << 'usa-table--stacked-header' if stacked_header
        classes.join(' ')
      end
    end
  end
end
