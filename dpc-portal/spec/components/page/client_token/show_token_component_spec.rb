# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::ClientToken::ShowTokenComponent, type: :component do
  include ComponentSupport
  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end

    let(:org) { ComponentSupport::MockOrg.new }
    let(:client_token) { { 'label' => 'Your token', 'token' => 'some-token' } }
    let(:component) { described_class.new(org, client_token) }
    let(:expected_html) do
      <<~HTML
        <div>
          <div class="margin-top-5 margin-bottom-5">← <a href="/organizations/#{org.path_id}?credential_start=true">Back to organization</a></div>
          <h1>Client token created</h1>
          <div>
            <div class="usa-alert usa-alert--warning margin-bottom-4">
              <div class="usa-alert__body">
                <h2 class="usa-alert__heading">Alert</h2>
                <p class="usa-alert__text">
                  This token won't be saved if you exit this screen.
                </p>
              </div>
            </div>
            <div class="margin-bottom-4">
              <label class="usa-label" for="token">Copy client token</label>
              <textarea name="token" id="token" rows="9" readonly="readonly" class="usa-textarea">
                some-token</textarea>
            </div>
            <a class="usa-button usa-button" href="/organizations/#{org.path_id}">Back to organization</a>
            <p class="margin-top-5"><a href="https://dpc.cms.gov/docsV1">View API Documentation</a></p>
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
