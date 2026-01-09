# frozen_string_literal: true

module Core
  module Table
    # Render a USWDS-styled table.
    class HeaderComponent < ViewComponent::Base
      attr_accessor :caption, :columns, :sorts

      def initialize(caption: '', columns: [], sorts: [])
        super
        @caption = caption
        @columns = columns
        @sorts = sorts
      end
    end
  end
end
