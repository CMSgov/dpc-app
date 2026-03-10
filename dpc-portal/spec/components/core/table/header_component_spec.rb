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

    context 'when the header has two cols' do
      let(:component) { described_class.new(caption: 'caption', columns: [{ label: 'A' }, { label: 'B' }]) }
      let(:expected_html) do
        <<~HTML
          <caption aria-hidden="true" hidden>caption</caption>
          <thead>
            <tr>
                <th scope="col">A</th>
                <th scope="col">B</th>
            </tr>
          </thead>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'when the header has a sorted row' do
      let(:component) do
        described_class.new(caption: 'caption', columns: [{ label: 'A' }, { label: 'B', sortable: true }])
      end
      let(:expected_html) do
        <<~HTML
          <caption aria-hidden="true" hidden>caption</caption>
          <thead>
            <tr>
                <th scope="col">A</th>
                <th data-sortable scope="col" aria-sort="descending">B</th>
            </tr>
          </thead>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end
  end
end
