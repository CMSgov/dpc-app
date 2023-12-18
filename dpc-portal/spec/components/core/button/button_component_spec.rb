# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::Button::ButtonComponent, type: :component do
    describe 'html' do
        subject(:html) do
            render_inline(component)
            rendered_content
        end

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
end
