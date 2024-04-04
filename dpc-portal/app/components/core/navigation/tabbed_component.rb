# frozen_string_literal: true

module Core
  module Navigation
    # Tabbed Navigation Component
    # ----------------
    #
    # [Based on USWDS](https://designsystem.digital.gov/components/header/extended/)
    #
    class TabbedComponent < ViewComponent::Base
      # links should be a list of tuples:
      # [text, selector_for_link, current?]
      # Note: if no link is current, no linked content will show on page load
      #       if more than one link is current, the content linked to the last link will show
      def initialize(header_id, links)
        super
        @header_id = header_id
        @start_index = -1
        links.each_with_index do |link, index|
          @start_index = index if link[2]
        end
        @links = links
        @selectors = links.map { |link| link[1] }
      end
    end
  end
end
