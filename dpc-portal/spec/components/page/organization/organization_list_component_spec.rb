# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Organization::OrganizationListComponent, type: :component do
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

      before do
        render_inline(component)
      end

      it 'should have an empty org search' do
        empty_org_search = <<~SELECT
          <select class="usa-select" name="org_search_selection" id="org_search_selection" onchange="filterOrgName(this);">
              <option value>Organization Name</option>
          </select>
        SELECT
        is_expected.to include(normalize_space(empty_org_search))
      end

      it 'should have an empty npi search' do
        empty_npi_search = <<~SELECT
          <select class="usa-select" name="npi_search_selection" id="npi_search_selection" onchange="filterNpi(this);">
              <option value>NPI</option>
          </select>
        SELECT
        is_expected.to include(normalize_space(empty_npi_search))
      end

      it 'should have an empty org search combo box' do
        is_expected.to include('<ul class="usa-card-group"></ul>')
      end
    end

    context 'when has one option' do
      let(:component) do
        org = double('org')
        allow(org).to receive(:name).and_return('name')
        allow(org).to receive(:npi).and_return('npi')
        described_class.new(organizations: [org])
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
                                  <form class="button_to" method="get" action="/portal/"><button class="usa-button" type="submit">View Details</button></form>
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

      it 'should have one org in search' do
        empty_org_search = <<~SELECT
          <select class="usa-select" name="org_search_selection" id="org_search_selection" onchange="filterOrgName(this);">
              <option value>Organization Name</option>
              <option value="name">name</option>
          </select>
        SELECT
        is_expected.to include(normalize_space(empty_org_search))
      end

      it 'should have one npi in search' do
        empty_npi_search = <<~SELECT
          <select class="usa-select" name="npi_search_selection" id="npi_search_selection" onchange="filterNpi(this);">
              <option value>NPI</option>
              <option value="npi">npi</option>
          </select>
        SELECT
        is_expected.to include(normalize_space(empty_npi_search))
      end

      it 'should have one card in list' do
        is_expected.to include(normalize_space(expected_html))
      end
    end
  end
end
