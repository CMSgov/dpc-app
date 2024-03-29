# frozen_string_literal: true

module Core
  module Navigation
    # Header Navigation Component Preview
    # ----------------
    #
    # [See at USWDS](https://designsystem.digital.gov/components/header/extended/)
    #
    class HeaderComponentPreview < ViewComponent::Preview
      def default
        links = [['Link 1', '/href1', true],
                 ['Link 2', '/href2', false]]
        render(Core::Navigation::HeaderComponent.new(links))
      end
    end
  end
end
