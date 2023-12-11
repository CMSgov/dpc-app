# frozen_string_literal: true

module Core
  module Card
    # Card Component
    # ----------------
    #
    # [See at USWDS](https://designsystem.digital.gov/components/card/)
    #
    class CardComponentPreview < ViewComponent::Preview
      # @param header "Header of the card"
      # @param body "Body of the card"
      # @param footer "Footer of the card"
      def default(header:, body:, footer:)
        render(Core::Card::CardComponent.new(header: header, body: body, footer: footer))
      end
    end
  end
end
