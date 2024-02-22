# frozen_string_literal: true

module Core
    module Prose
      # Render a USWDS-styled prose.
      class ProseComponentPreview < ViewComponent::Preview
        DEFAULT_INNER_HTML = <<~HTML
        <h1>Title</h1>
        <p>Some paragraph</p>
      HTML
        def default()
            render Core::Prose::ProseComponent.new() do
                raw DEFAULT_INNER_HTML
            end
        end
      end
    end
  end
  