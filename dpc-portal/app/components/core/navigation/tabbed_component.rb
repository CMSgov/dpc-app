# frozen_string_literal: true

module Core
  module Navigation
    # Tabbed Navigation Component
    # ----------------
    #
    # [See at USWDS](https://designsystem.digital.gov/components/header/extended/)
    #
    class TabbedComponent < ViewComponent::Base
      # links should be a list of tuples:
      # [text, href, selector_for_link, current?]
      # href in case javascript disabled
      def initialize(header_id, links)
        super
        @header_id = header_id
        @start_index = -1
        links.each_with_index do |link, index|
          @start_index = index if link[3]
        end
        @links = links
        @selectors = links.map { |link| link[2] }
      end
    end
  end
end
