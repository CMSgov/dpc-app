# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::Button::ButtonComponent, type: :component do
  describe 'html' do
    subject(:html) do
      render_inline(component)
      rendered_content
    end

    context 'with default button class' do
      let(:component) { described_class.new(label: 'label', destination: 'destination') }
      let(:expected_html) do
        <<~HTML
          <form class="button_to" method="get" action="destination"><button class="usa-button" type="submit">label</button></form>
        HTML
      end

      before do
        render_inline(component)
      end

      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'with outline button class' do
      let(:component) do
        described_class.new(label: 'label', destination: 'destination', additional_classes: ['usa-button--outline'])
      end
      let(:expected_html) do
        <<~HTML
          <form class="button_to" method="get" action="destination"><button class="usa-button--outline usa-button" type="submit">label</button></form>
        HTML
      end
      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'disabled' do
      let(:component) do
        described_class.new(label: 'label', destination: 'destination', disabled: true)
      end
      let(:expected_html) do
        <<~HTML
          <form class="button_to" method="get" action="destination"><button class="usa-button" disabled="disabled" type="submit">label</button></form>
        HTML
      end
      it { is_expected.to match_html_fragment(expected_html) }
    end
  end
end
