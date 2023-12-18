# frozen_string_literal: true

require 'rails_helper'

# https://designsystem.digital.gov/components/table/
#
# TODO:
# - scrollable
RSpec.describe Core::Table::TableComponent, type: :component do
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

    context 'when the table has additional classes' do
      let(:component) { described_class.new(additional_classes: ['width-full']) }
      let(:expected_html) do
        <<~HTML
          <table id="" class="width-full usa-table">
          </table>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'when the table is sortable' do
      let(:component) { described_class.new(sortable: true) }
      let(:expected_html) do
        <<~HTML
          <table id="" class="usa-table">
          </table>
          <div class="usa-sr-only usa-table__announcement-region" aria-live="polite"></div>
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
