# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::Card::BasicComponent, type: :component do
  describe 'html' do
    subject(:html) do
      render_inline(component)
      rendered_content
    end

    let(:component) { described_class.new }
    let(:expected_html) do
      <<~HTML
        <div>
          <div class="shadow-5 border-1px border-base-lighter radius-md margin-bottom-4 padding-x-3 padding-bottom-3">
            <div class="display-flex flex-row flex-justify">
              <div class="flex-align-self-start">
                <h1>Welcome</h1>
              </div>
              <div class="flex-align-self-end">
              </div>
            </div>
          </div>
        </div>
      HTML
    end

    before do
      render_inline(component)
    end

    it { is_expected.to match_html_fragment(expected_html) }

    context 'when has button' do
      let(:component) do
        described_class.new(text_content: '<h1>Yo</h1>', button_params: { name: 'Doit', path: '/there', method: :post })
      end
      let(:expected_html) do
        <<~HTML
          <div>
            <div class="shadow-5 border-1px border-base-lighter radius-md margin-bottom-4 padding-x-3 padding-bottom-3">
              <div class="display-flex flex-row flex-justify">
                <div class="flex-align-self-start">
                  <h1>Yo</h1>
                </div>
                <div class="flex-align-self-end">
                  <form class="button_to" method="post" action="/there">
                   <button class="usa-button" type="submit">Doit</button>
                  </form>#{'                  '}
                </div>
              </div>
            </div>
          </div>
        HTML
      end
      it { is_expected.to match_html_fragment(expected_html) }
    end
  end
end
