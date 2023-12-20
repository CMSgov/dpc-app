# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::Table::RowComponent, type: :component do
  describe 'html' do
    subject(:html) do
      render_inline(component)
      rendered_content
    end

    let(:obj) { { 'a' => 'First', 'b' => 'Second', 'id' => 'some-guid' } }
    let(:component) { described_class.with_collection([obj], keys: %w[a b]) }
    let(:expected_html) do
      <<~HTML
        <tbody>
          <tr>
            <td data-sort-value="First">First</td>
            <td data-sort-value="Second">Second</td>
          </tr>
        </tbody>
      HTML
    end

    before do
      render_inline(component)
    end

    it { is_expected.to match_html_fragment(expected_html) }

    context 'with delete' do
      let(:component) { described_class.with_collection([obj], keys: %w[a b], delete_path: '/foo/bar') }
      let(:expected_html) do
        <<~HTML
          <form class="button_to" method="post" action="/foo/bar/some-guid"><input type="hidden" name="_method" value="delete" autocomplete="off" /><button class="usa-button" type="submit">Yes, revoke token</button></form>
        HTML
      end
      it { is_expected.to include(expected_html) }
    end

    context 'with two rows' do
      let(:component) { described_class.with_collection([obj, obj], keys: %w[a b]) }
      let(:expected_html) do
        <<~HTML
          <tbody>
            <tr>
              <td data-sort-value="First">First</td>
              <td data-sort-value="Second">Second</td>
            </tr>
            <tr>
              <td data-sort-value="First">First</td>
              <td data-sort-value="Second">Second</td>
            </tr>
          </tbody>
        HTML
      end
      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'with date' do
      let(:obj) { { 'a' => '2023-12-12 18:58', 'b' => 'Second' } }
      let(:expected_html) do
        <<~HTML
          <tbody>
            <tr>
              <td data-sort-value="12/12/2023 at  6:58PM UTC">12/12/2023 at 6:58PM UTC</td>
              <td data-sort-value="Second">Second</td>
            </tr>
          </tbody>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end
  end
end
