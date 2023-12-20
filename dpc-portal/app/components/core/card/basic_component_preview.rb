# frozen_string_literal: true

module Core
  module Card
    # Basic Card Component
    class BasicComponentPreview < ViewComponent::Preview
      # button must have name, path, and method keys
      #
      # @param text_content textarea
      # @param button_name textarea
      # @param inner_html textarea
      def parameterized(text_content: '<h1>Welcome</h1>', button_name: 'Go to thing',
                        inner_html: '<div>Some Content</div>')
        button_params = button_name.present? ? { name: button_name, method: :get, path: '/' } : nil
        render Core::Card::BasicComponent.new(text_content: text_content, button_params: button_params) do
          raw inner_html
        end
      end
    end
  end
end
