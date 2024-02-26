# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::Prose::ProseComponent, type: :component do
  describe 'html' do
    subject(:html) do
      render_inline(component)
      rendered_content
    end

    let(:component) { described_class.new }
    let(:expected_html) do
      <<~HTML
        <div class="usa-prose"></div>
      HTML
    end

    before do
      render_inline(component)
    end

    it { is_expected.to match_html_fragment(expected_html) }
  end
end
