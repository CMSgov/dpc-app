# frozen_string_literal: true

module Core
  module Button
    # Button
    # ----------------
    #
    # [See at USWDS](https://designsystem.digital.gov/components/button/)
    #
    class ButtonComponentPreview < ViewComponent::Preview
      # @param label "Label of the button"
      # @param destination "Destination when button is clicked"
      # @after_render :wrap_in_ul
      def default(label: 'Text', destination: root_path)
        render(Core::Button::ButtonComponent.new(label: label, destination: destination))
      end

      private

      def wrap_in_ul(html, _)
        <<~HTML
          <ul class="usa-card-group">
              #{html}
          </ul>
        HTML
      end
    end
  end
end
