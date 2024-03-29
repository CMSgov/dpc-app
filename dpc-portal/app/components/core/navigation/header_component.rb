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
      # [text, href, current?]
      def initialize(links)
        super
        @links = links
      end
    end
  end
end
