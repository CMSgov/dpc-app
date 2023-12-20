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
          <div class="margin-bottom-5">← <a href="/portal/organizations/#{org.path_id}">#{org.name}</a></div>
          <h1>Client token created</h1>
          <div>
            <h2>"Your token" created for #{org.name}</h2>
            <hr/>
            <div class="margin-bottom-4">
              <label class="usa-label" for="token">Your token</label>
              <textarea name="token" id="token" rows="9" readonly="readonly" class="usa-textarea">
                some-token
              </textarea>
            </div>
            <div class="usa-alert usa-alert--warning margin-bottom-4">
              <div class="usa-alert__body">
                <p class="usa-alert__text">
                  Copy or download your token right now! You won't be able to see it again.
                </p>
              </div>
            </div>
            <form class="button_to" method="get" action="http://test.host/portal/">
              <button class="usa-button" type="submit">Return to portal</button>
            </form>
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
