# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::Form::TextInputComponent, type: :component do
  describe 'html' do
    subject(:html) do
      render_inline(component)
      rendered_content
    end

    let(:component) { described_class.new(label: 'Some Label', attribute: 'attr') }
    let(:expected_html) do
      <<~HTML
        <div class="margin-bottom-4">
          <label class="usa-label" for="attr">Some Label</label>
          <input type="text" name="attr" id="attr" value="" class="usa-input" />
         </div>
      HTML
    end

    before do
      render_inline(component)
    end

    it { is_expected.to match_html_fragment(expected_html) }

    context 'has hint' do
      let(:component) { described_class.new(label: 'Some Label', attribute: 'attr', hint: 'Hint') }
      let(:expected_html) do
        <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="attr">Some Label</label>
            <p class="usa-hint">Hint</p>
            <input type="text" name="attr" id="attr" value="" class="usa-input" />
           </div>
        HTML
      end
      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'has more classes' do
      let(:input_options) { { class: ['custom-class'] } }
      let(:component) { described_class.new(label: 'Some Label', attribute: 'attr', input_options: input_options) }
      let(:expected_html) do
        <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="attr">Some Label</label>
            <input type="text" name="attr" id="attr" value="" class="custom-class usa-input" />
           </div>
        HTML
      end
      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'has max length' do
      let(:input_options) { { maxlength: 25 } }
      let(:component) { described_class.new(label: 'Some Label', attribute: 'attr', input_options: input_options) }
      let(:expected_html) do
        <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="attr">Some Label</label>
            <input type="text" name="attr" id="attr" value="" class="usa-input" maxlength="25" />
           </div>
        HTML
      end
      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'error' do
      let(:component) { described_class.new(label: 'Some Label', attribute: 'attr', error_msg: 'Bad Input') }
      let(:expected_html) do
        <<~HTML
          <div class="margin-bottom-4">
            <label class="usa-label" for="attr">Some Label</label>
            <p style="color: #b50909;">Bad Input</p>
            <input type="text" name="attr" id="attr" value="" class="usa-input usa-input--error" />
           </div>
        HTML
      end
      it { is_expected.to match_html_fragment(expected_html) }
    end
  end
end
