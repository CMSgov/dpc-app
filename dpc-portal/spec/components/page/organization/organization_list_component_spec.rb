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
      let(:component) { described_class.new(ao_or_cd: :ao, links: []) }

      before do
        render_inline(component)
      end

      it 'should have reference to Authorized Official' do
        is_expected.to include(normalize_space(ao_ref))
      end
    end
    context 'when has no options and is cd' do
      let(:component) { described_class.new(ao_or_cd: :cd, links: []) }

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
        org = ProviderOrganization.new(name: 'name', npi: 'npi', id: '5', terms_of_service_accepted_at: 2.days.ago)
        link = CdOrgLink.new(provider_organization: org)
        described_class.new(ao_or_cd: :cd, links: [link])
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
                          <div class="clearfix">
                          <div id="npi_div" class="float-left">
                          <p class="usa-card__text">
                              <span style="font-weight:bold">NPI</span>
                              <span>npi</span>
                          </p>
                          </div>
                          <div id="status_div" class="float-right">
                          <p class="usa-card__text">
                              <form class="button_to" method="get" action="/portal/organizations/5"><button class="usa-button--outline usa-button" type="submit">View Details</button></form>
                          </p>
                          </div>
                      </div>
                  <div class="clearfix">
                    <div class="float-left">  <svg class="text-accent-cool usa-icon" style="transform: scale(1)" aria-hidden="true" role="img">
                        <use xlink:href=/portal/assets/@uswds/uswds/dist/img/sprite-9865eea7b251e43137fb770626d6cd51c474a3a436678a6e66cafce50968076f.svg#verified></use>
                      </svg>
                    </div>
                    <div class="float-left margin-left-1 margin-top-neg-2px">Manage your organization.</div>
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
