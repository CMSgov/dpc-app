# frozen_string_literal: true

module Core
    module Card
      # Render a USWDS-styled card.
      class CardComponent < ViewComponent::Base
        attr_accessor :header, :footer, :body
  
        def initialize(header: '', footer: '', body: '')
          super
          @header = header
          @footer = footer
          @body = body
        end
      end
    end
  end