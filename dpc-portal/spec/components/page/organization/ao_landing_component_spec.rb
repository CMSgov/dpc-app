# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Organization::AoLandingComponent, type: :component do
  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end

    def normalize_space(str)
      str.gsub(/^ +/, '').gsub("\n", '')
    end

    context 'when has no options' do
      let(:component) { described_class.new(organizations: []) }
      let(:html) do
        <<~HTML
          <div>
            <div class="margin-bottom-5">&larr; link</div>
            <div class="usa-prose">
                <h1>Welcome to the DPC Portal</h1>
                <p style="max-width: none">
                As an Authorized Official (AO), use this portal to manage access to Medicare claims data
                through the Data at the Point of Care (DPC) application programming interface (API)
                </p>
                <div class="display-flex flex-row flex-justify">
                    <div class="flex-align-self-center">
                    <h2>My organizations</h2>
                    </div>
                    <div class="flex-align-self-center">
                        <form class="button_to" method="get" action="/portal/">
                            <button class="usa-button" type="submit">Add new organization</button>
                        </form>
                    </div>
                </div>
                <p style="max-width: none">
                These are organizations for which you serve as an AO.#{' '}
                Organizations cannot access Medicare claims data without an AO agreement to follow terms of service.
                </p>
                <p>You don't have any organizations to show.</p>
            </div>
          </div>
        HTML
      end

      before do
        render_inline(component)
      end

      it 'should have a no organization message' do
        is_expected.to include(normalize_space(html))
      end
    end

    context 'when has one option' do
      let(:component) do
        org = double('org')
        allow(org).to receive(:name).and_return('name')
        allow(org).to receive(:npi).and_return('npi')
        allow(org).to receive(:api_id).and_return('api')
        described_class.new(organizations: [org])
      end
      let(:expected_card_html) do
        <<~HTML
          <ul class="usa-card-group">
              <li class="usa-card tablet-lg:grid-col-1 widescreen:grid-col-1" style="list-style:none; visibility:visible;" data-npi="npi" data-name="name">
                  <div class="usa-card__container">
                      <div class="usa-card__header">
                          <h2 class="usa-card__heading">
                              name
                          </h2>
                      </div>
                      <div class="usa-card__body">
                          <div id="npi_div" style="float:left">
                              <p class="usa-card__text">
                                  <span style="font-weight:bold">NPI</span>
                                  <span>npi</span>
                              </p>
                          </div>
                          <div id="status_div" style="float:right">
                              <p class="usa-card__text">
                                  <form class="button_to" method="get" action="/portal/organizations/api"><button class="usa-button--outline usa-button" type="submit">View Details</button></form>
                              </p>
                          </div>
                      </div>
                  </div>
              </li>
          </ul>
        HTML
      end

      before do
        render_inline(component)
      end

      it 'should have one card in list' do
        is_expected.to include(normalize_space(expected_card_html))
      end

      it 'should not show \'no organiztions to show message\'' do
        is_expected.not_to include('<p>You don\'t have any organizations to show.</p>')
      end
    end
  end
end
