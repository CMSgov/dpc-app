# frozen_string_literal: true

module Core
  module Table
    # Render a USWDS-styled table.
    class HeaderComponent < ViewComponent::Base
      attr_accessor :caption, :columns, :sort

      def initialize(caption: '', columns: [], sort: -1)
        super
        @caption = caption
        @columns = columns
        @sort = sort
      end
    end
  end
end
