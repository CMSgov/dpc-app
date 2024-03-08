# frozen_string_literal: true

module Core
  module Table
    # Table Component
    # ----------------
    #
    # [See at USWDS](https://designsystem.digital.gov/components/table/)
    #
    class TableComponentPreview < ViewComponent::Preview
      DEFAULT_INNER_HTML = <<~HTML
        <caption>
          A table shows information in columns and rows.
        </caption>
        <thead>
          <tr>
            <th scope='col'>Document title</th>
            <th scope='col'>Description</th>
            <th scope='col'>Year</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <th scope='row'>
              Declaration of Independence
            </th>
            <td>
              Statement adopted by the Continental Congress declaring independence
              from the British Empire.
            </td>
            <td>1776</td>
          </tr>
          <tr>
            <th scope='row'>
              Bill of Rights
            </th>
            <td>
              The first ten amendments of the U.S. Constitution guaranteeing
              rights and freedoms.
            </td>
            <td>1791</td>
          </tr>
          <tr>
            <th scope='row'>
              Declaration of Sentiments
            </th>
            <td>
              A document written during the Seneca Falls Convention outlining
              the rights that American women should be entitled to as citizens.
            </td>
            <td>1848</td>
          </tr>
        </tbody>
      HTML

      # `sortable` does not show up in the Preview, but does show up in the HTML.
      # It adds a div with an "aria-live" attribute that uswds requires for accessibility
      #
      # @param sortable toggle
      # @param inner_html textarea
      # @param additional_classes textarea space-delimited
      def parameterized(additional_classes: '', sortable: false, inner_html: DEFAULT_INNER_HTML)
        more_classes = additional_classes.present? ? additional_classes.split : []
        render Core::Table::TableComponent.new(additional_classes: more_classes, sortable:) do
          raw inner_html
        end
      end
    end
  end
end
