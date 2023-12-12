# frozen_string_literal: true

require 'rails_helper'

# https://designsystem.digital.gov/components/table/
#
# TODO:
# - scrollable
RSpec.describe Core::Table::Component, type: :component do
  describe 'html' do
    subject(:html) do
      render_inline(component)
      rendered_content
    end

    let(:component) { described_class.new }
    let(:expected_html) do
      <<~HTML
        <table id="" class="usa-table">
        </table>
      HTML
    end

    before do
      render_inline(component)
    end

    it { is_expected.to match_html_fragment(expected_html) }

    context 'when the table has the default settings' do
      let(:component) { described_class.new }
      let(:expected_html) do
        <<~HTML
          <table id="" class="usa-table">
          </table>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'when the table is striped' do
      let(:component) { described_class.new(striped: true) }
      let(:expected_html) do
        <<~HTML
          <table id="" class="usa-table usa-table--striped">
          </table>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'when the table is borderless' do
      let(:component) { described_class.new(borderless: true) }
      let(:expected_html) do
        <<~HTML
          <table id="" class="usa-table usa-table--borderless">
          </table>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'when the table is stacked' do
      let(:component) { described_class.new(stacked: true) }
      let(:expected_html) do
        <<~HTML
          <table id="" class="usa-table usa-table--stacked">
          </table>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'when the table has a stacked header' do
      let(:component) { described_class.new(stacked_header: true) }
      let(:expected_html) do
        <<~HTML
          <table id="" class="usa-table usa-table--stacked-header">
          </table>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'when the table has an id' do
      let(:component) { described_class.new(id: 'table42') }
      let(:expected_html) do
        <<~HTML
          <table class="usa-table" id="table42">
          </table>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end
  end
end
