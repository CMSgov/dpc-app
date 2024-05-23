# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Organization::OrganizationListComponent, type: :component do
  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end

    let(:ao_ref) { 'As an Authorized Official' }
    def normalize_space(str)
      str.gsub(/^ +/, '').gsub("\n", '')
    end

    context 'when is ao' do
      let(:component) { described_class.new(ao_or_cd: :ao, organizations: []) }

      before do
        render_inline(component)
      end

      it 'should have reference to Authorized Official' do
        is_expected.to include(normalize_space(ao_ref))
      end
    end
    context 'when has no options and is cd' do
      let(:component) { described_class.new(ao_or_cd: :cd, organizations: []) }

      before do
        render_inline(component)
      end

      it 'should have not reference to Authorized Official' do
        is_expected.to_not include(normalize_space(ao_ref))
      end
      it 'should say no organizations' do
        empty_npi_search = "<p>You don't have any organizations to show.</p>"
        is_expected.to include(normalize_space(empty_npi_search))
      end
    end

    context 'when has one option' do
      let(:component) do
        org = double('org')
        allow(org).to receive(:name).and_return('name')
        allow(org).to receive(:npi).and_return('npi')
        allow(org).to receive(:api_id).and_return('api')
        described_class.new(ao_or_cd: :cd, organizations: [org])
      end
      let(:expected_html) do
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
        is_expected.to include(normalize_space(expected_html))
      end
    end
  end
end
