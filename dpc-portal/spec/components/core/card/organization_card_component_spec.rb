# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::Card::OrganizationCardComponent, type: :component do
  describe 'html' do
    subject(:html) do
      render_inline(component)
      rendered_content
    end

    let(:component) do
      org = double('org')
      allow(org).to receive(:name).and_return('name')
      allow(org).to receive(:npi).and_return('npi')
      allow(org).to receive(:api_id).and_return('api')
      described_class.new(organization: org)
    end
    let(:expected_html) do
      <<~HTML
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
                    <form class="button_to" method="get" action="/portal/organizations/api"><button class="usa-button" type="submit">View Details</button></form>
                </p>
                </div>
            </div>
            </div>
        </li>
      HTML
    end

    before do
      render_inline(component)
    end

    it { is_expected.to match_html_fragment(expected_html) }
  end
end
