# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::Table::HeaderComponent, type: :component do
  describe 'html' do
    subject(:html) do
      render_inline(component)
      rendered_content
    end

    let(:component) { described_class.new }
    let(:expected_html) do
      <<~HTML
        <caption aria-hidden="true" hidden></caption>
        <thead>
          <tr>
          </tr>
        </thead>
      HTML
    end

    before do
      render_inline(component)
    end

    it { is_expected.to match_html_fragment(expected_html) }

    context 'when the header has the default settings' do
      let(:component) { described_class.new }
      let(:expected_html) do
        <<~HTML
          <caption aria-hidden="true" hidden></caption>
          <thead>
            <tr>
            </tr>
          </thead>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'when the header has the default settings' do
      let(:component) { described_class.new }
      let(:expected_html) do
        <<~HTML
          <caption aria-hidden="true" hidden></caption>
          <thead>
            <tr>
            </tr>
          </thead>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'when the header has two rows' do
      let(:component) { described_class.new(caption: 'caption', columns: %w[A B]) }
      let(:expected_html) do
        <<~HTML
          <caption aria-hidden="true" hidden>caption</caption>
          <thead>
            <tr>
                <th scope="row" role="columnheader">A</th>
                <th scope="row" role="columnheader">B</th>
            </tr>
          </thead>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'when the header has a sorted row' do
      let(:component) { described_class.new(caption: 'caption', columns: %w[A B], sort: 1) }
      let(:expected_html) do
        <<~HTML
          <caption aria-hidden="true" hidden>caption</caption>
          <thead>
            <tr>
                <th scope="row" role="columnheader">A</th>
                <th data-sortable scope="row" role="columnheader" aria-sort="ascending">B</th>
            </tr>
          </thead>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end
  end
end
