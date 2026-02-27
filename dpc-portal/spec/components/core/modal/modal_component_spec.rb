# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::Modal::ModalComponent, type: :component do
  include ComponentSupport
  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end

    let(:component) do
      described_class.new('For reals?',
                          'Are you sure?',
                          '<a href="#" class="usa-button">Yes</a>',
                          'No',
                          'verify-modal')
    end
    let(:expected_html) do
      <<~HTML
        <div class="usa-modal" id="verify-modal" aria-labelledby="verify-modal-heading" aria-describedby="verify-modal-description">
          <div class="usa-modal__content">
            <div class="usa-modal__main">
              <h2 class="usa-modal__heading" id="verify-modal-heading">
                For reals?
              </h2>
              <div class="usa-prose">
                <p id="verify-modal-description">
                  Are you sure?
                </p>
              </div>
              <div class="usa-modal__footer">
                <ul class="usa-button-group">
                  <li class="usa-button-group__item">
                    <a href="#" class="usa-button">Yes</a>
                  </li>
                  <li class="usa-button-group__item">
                    <button type="button" class="usa-button usa-button--unstyled padding-105 text-center" data-close-modal>
                      No
                    </button>
                  </li>
                </ul>
              </div>
            </div>
            <button type="button" class="usa-button usa-modal__close" aria-label="Close this window" data-close-modal>
              <svg class="usa-icon" style="transform: scale(1)" aria-hidden="true" role="img">
                <use xlink:href="/assets/@uswds/uswds/dist/img/sprite-9865eea7b251e43137fb770626d6cd51c474a3a436678a6e66cafce50968076f.svg#close"></use>
              </svg>
            </button>
          </div>
        </div>
      HTML
    end
    before do
      render_inline(component)
    end

    it { is_expected.to match_html_fragment(expected_html) }
  end
end
