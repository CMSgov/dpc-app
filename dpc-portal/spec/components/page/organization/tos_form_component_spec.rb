# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Organization::TosFormComponent, type: :component do
  include ComponentSupport
  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end

    let(:organization) { create(:provider_organization, name: 'Health Hut') }
    let(:component) { described_class.new(organization) }
    let(:expected_html) do
      <<~HTML
        <div>
          <div class="margin-bottom-5">← <a href="/portal/">path</a></div>
          <h1>Add new organization</h1>
          <h2>Sign Terms of Service</h2>
          <p>Confirmed! We succesfully identified you as an Authorized Official (AO) of Health Hut (NPI #{organization.npi}).</p>
          <p>To manage this organization's credentials, you must agree to the DPC API's Terms of Service.</p>
          <div style="width: 100%; height: 200px; background-color: silver" class="margin-bottom-5">&nbsp;</div>
          <div>
            <ul class="usa-button-group">
              <li class="usa-button-group__item">
                <form class="button_to" method="post" action="/portal/organizations/#{organization.to_param}/sign_tos"><button class="usa-button" type="submit">I have read and accepted the Terms of Service</button></form>
              </li>
              <li class="usa-button-group__item">
                <button type="button" class="usa-button usa-button--unstyled padding-105 text-center" data-close-modal>
                  <a href="/portal/organizations">Cancel</a>
                </button>
              </li>
            </ul>
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
