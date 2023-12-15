# frozen_string_literal: true

module Core
  module Table
    # Render a USWDS-styled table.
    class RowComponentPreview < ViewComponent::Preview
      # @after_render :wrap_in_table
      #
      #
      # @param values textarea comma-delimited values
      def parameterized(values: 'First,Second,Third')
        obj = {}
        attributes = []
        (values || '').split(',').each_with_index do |v, i|
          obj[i.to_s] = v
          attributes << i.to_s
        end

        render(Core::Table::RowComponent.with_collection([obj], keys: attributes))
      end

      private

      def wrap_in_table(html, _context)
        <<~HTML
           <table class="usa-table width-full">
             <caption aria-hidden="true" hidden>Example Table</caption>
            #{html}
          </table>
        HTML
      end
    end
  end
end
