# frozen_string_literal: true

module Core
  module Navigation
    # Header Navigation Component Preview
    # ----------------
    #
    # [See at USWDS](https://designsystem.digital.gov/components/header/extended/)
    # @after_render :add_sections
    #
    class HeaderComponentPreview < ViewComponent::Preview
      def default
        links = [['Link 1', '.area_1', true],
                 ['Link 2', '.area_2', false]]
        render(Core::Navigation::HeaderComponent.new(links))
      end

      private

      def add_sections(html, _context)
        <<~HTML
          #{html}
          <div class="area_1">
          <h2>AREA 1</h2>
          </div>
          <div class="area_2">
          <h2>AREA 2</h2>
          </div>
        HTML
      end
    end
  end
end
