# frozen_string_literal: true

module Core
  module Table
    # Header Component for sortable table
    class HeaderComponentPreview < ViewComponent::Preview
      # @after_render :wrap_in_table
      #
      # @param caption textarea
      # @param columns textarea comma-delimited
      # @param sort_indexes textarea comma-delimited
      def parameterized(caption: 'caption', columns: nil, sort_indexes: nil)
        column_titles = columns.present? ? columns.split(',') : %w[A B]
        sorts = sort_indexes.present? ? sort_indexes.split(',').map(&:to_i) : []
        render(Core::Table::HeaderComponent.new(caption:, columns: column_titles, sorts:))
      end

      private

      def wrap_in_table(html, context)
        <<~HTML
          <table class="usa-table width-full">
            <caption aria-hidden="true" hidden>Example Table</caption>
           #{html}
          <tbody>
           #{build_rows(context)}
                   </tbody>#{' '}
                   </table>
          <div class="usa-sr-only usa-table__announcement-region" aria-live="polite"></div>
        HTML
      end

      def build_rows(context)
        rows = []
        %w[A B C D].each do |letter|
          row = ['<tr>']
          (context.dig(:params, 'columns') || 'a,b').split(',').size.times do
            row << %(<td data-sort-value="#{letter}">#{letter}</td>)
          end
          row << '</tr>'
          rows << row.join
        end
        rows.join
      end
    end
  end
end
