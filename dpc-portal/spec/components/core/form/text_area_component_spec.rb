# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::Form::TextAreaComponent, type: :component do
  describe 'html' do
    subject(:html) do
      render_inline(component)
      rendered_content
    end

    before do
      render_inline(component)
    end

    context 'basic' do
      let(:component) { described_class.new(label: 'Some Label', attribute: 'attr') }
      let(:expected_html) do
        <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="attr">Some Label</label>
            <textarea name="attr" id="attr" class="usa-textarea"/>
          </div>
        HTML
      end
      it { is_expected.to match_html_fragment(expected_html) }
    end
    context 'with hint' do
      let(:component) { described_class.new(label: 'Some Label', attribute: 'attr', hint: 'Here is a hint') }
      let(:expected_html) do
        <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="attr">Some Label</label>
            <p class="usa-hint">Here is a hint</p>
            <textarea name="attr" id="attr" class="usa-textarea"/>
          </div>
        HTML
      end
      it { is_expected.to match_html_fragment(expected_html) }
    end
    context 'readonly' do
      let(:input_options) { { readonly: :readonly } }
      let(:component) { described_class.new(label: 'Some Label', attribute: 'attr', input_options: input_options) }
      let(:expected_html) do
        <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="attr">Some Label</label>
            <textarea name="attr" id="attr" readonly="readonly" class="usa-textarea"/>
          </div>
        HTML
      end
      it { is_expected.to match_html_fragment(expected_html) }
    end
    context 'with default' do
      let(:component) { described_class.new(label: 'Some Label', attribute: 'attr', default: 'Lorem...') }
      let(:expected_html) do
        <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="attr">Some Label</label>
            <textarea name="attr" id="attr" class="usa-textarea">
              Lorem...
            </textarea>
          </div>
        HTML
      end
      it { is_expected.to match_html_fragment(expected_html) }
    end
  end
end
