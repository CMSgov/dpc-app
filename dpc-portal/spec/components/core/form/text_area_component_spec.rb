# frozen_string_literal: true

require "rails_helper"

RSpec.describe Core::Form::TextAreaComponent, type: :component do
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
  end
end
