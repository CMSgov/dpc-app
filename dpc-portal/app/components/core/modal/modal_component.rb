# frozen_string_literal: true

module Core
  module Modal
    # Render a USWDS-styled modal.
    class ModalComponent < ViewComponent::Base
      attr_reader :heading, :description, :yes_action, :no_message, :modal_id

      # prompt: text on button on page
      def initialize(heading, description, yes_action, no_message, modal_id)
        super
        @heading = heading
        @description = description
        @yes_action = yes_action
        @no_message = no_message
        @modal_id = modal_id
      end
    end
  end
end
