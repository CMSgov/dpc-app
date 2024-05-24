# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::Icon::UswdsComponent, type: :component do
  describe 'html' do
    subject(:html) do
      render_inline(component)
      rendered_content
    end
    let(:component) { described_class.new('lock') }

    let(:expected_html) do
      <<~HTML
        <svg class="usa-icon" style="transform: scale(1)" aria-hidden="true" role="img">
          <use xlink:href=/portal/assets/@uswds/uswds/dist/img/sprite-9865eea7b251e43137fb770626d6cd51c474a3a436678a6e66cafce50968076f.svg#lock></use>
        </svg>
      HTML
    end

    before do
      render_inline(component)
    end

    it { is_expected.to match_html_fragment(expected_html) }

    context 'changed size' do
      let(:component) { described_class.new('circle_check', size: 2) }
      let(:expected_html) do
        <<~HTML
          <svg class="usa-icon" style="transform: scale(2)" aria-hidden="true" role="img">
            <use xlink:href=/portal/assets/@uswds/uswds/dist/img/sprite-9865eea7b251e43137fb770626d6cd51c474a3a436678a6e66cafce50968076f.svg#circle_check></use>
          </svg>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end
    context 'added classes' do
      let(:component) { described_class.new('lock', additional_classes: ['foo', 'bar']) }
      let(:expected_html) do
        <<~HTML
          <svg class="foo bar usa-icon" style="transform: scale(1)" aria-hidden="true" role="img">
            <use xlink:href=/portal/assets/@uswds/uswds/dist/img/sprite-9865eea7b251e43137fb770626d6cd51c474a3a436678a6e66cafce50968076f.svg#lock></use>
          </svg>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end
  end
end
