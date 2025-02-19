# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::Form::MaskedInputComponent, type: :component do
  describe 'html' do
    subject(:html) do
      render_inline(component)
      rendered_content
    end

    before do
      render_inline(component)
    end

    context 'us-phone mask' do
      let(:component) { described_class.new(label: 'Some Label', attribute: 'attr', mask: 'us-phone') }
      let(:expected_html) do
        <<~HTML
          <label class="usa-label" for="attr">Some Label</label>
          <input type="text" name="attr" id="attr" value="" class="usa-input usa-masked"
           placeholder="___-___-____" pattern="\\d{3}-\\d{3}-\\d{4}" aria-describedby="telHint" />
        HTML
      end
      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'invalid mask' do
      let(:component) { described_class.new(label: 'Some Label', attribute: 'attr', mask: :foo) }
      let(:expected_html) do
        <<~HTML
          <label class="usa-label" for="attr">Some Label</label>
          <input type="text" name="attr" id="attr" value="" class="usa-input usa-masked" />
        HTML
      end
      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'hint' do
      let(:component) { described_class.new(label: 'Some Label', attribute: 'attr', mask: :foo, hint: 'Hint') }
      let(:expected_html) do
        <<~HTML
           <label class="usa-label" for="attr">Some Label</label>
          <p class="usa-hint">Hint</p>
           <input type="text" name="attr" id="attr" value="" class="usa-input usa-masked" />
        HTML
      end
      it { is_expected.to match_html_fragment(expected_html) }
    end

    context 'error' do
      let(:component) do
        described_class.new(label: 'Some Label', attribute: 'attr', mask: :foo, error_msg: 'Bad Input')
      end
      let(:expected_html) do
        <<~HTML
          <label class="usa-label" for="attr">Some Label</label>
          <p style="color: #b50909;">Bad Input</p>
          <input type="text" name="attr" id="attr" value="" class="usa-input usa-masked usa-input--error" />
        HTML
      end
      it { is_expected.to match_html_fragment(expected_html) }
    end
  end
end
