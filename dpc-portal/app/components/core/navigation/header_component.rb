# frozen_string_literal: true

module Core
  module Navigation
    # Header Navigation Component
    # ----------------
    #
    # [See at USWDS](https://designsystem.digital.gov/components/header/extended/)
    #
    class HeaderComponent < ViewComponent::Base
      # links should be a list of tuples:
      # [text, selector_for_link, current?]
      def initialize(links)
        super
        @links = links
        @selectors = links.map { |link| link[1] }
      end
    end
  end
end
