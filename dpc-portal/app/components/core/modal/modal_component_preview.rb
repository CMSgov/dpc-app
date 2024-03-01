# frozen_string_literal: true

module Core
  module Modal
    # Modal Component
    # ----------------
    #
    # [See at USWDS](https://designsystem.digital.gov/components/modal/)
    #
    class ModalComponentPreview < ViewComponent::Preview
      # @after_render :add_launcher
      def default
        render(Core::Modal::ModalComponent.new('For reals?', 'Are you sure?',
                                               '<a href="#" class="usa-button">Yes</a>', 'No',
                                               'verify-modal'))
      end

      private

      def add_launcher(html, _context)
        <<~HTML
          <a href="#verify-modal" aria-controls="verify-modal" data-open-modal="data-open-modal" class="usa-button">
            Do thing
          </a>
          #{html}
        HTML
      end
    end
  end
end
