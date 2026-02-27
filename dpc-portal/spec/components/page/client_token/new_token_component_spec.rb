# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::ClientToken::NewTokenComponent, type: :component do
  include ComponentSupport
  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end

    let(:org) { ComponentSupport::MockOrg.new }

    let(:component) { described_class.new(org) }
    let(:expected_html) do
      <<~HTML
        <div>
          <div class="margin-bottom-5">← <a href="/portal/organizations/#{org.path_id}">Back to organization</a></div>
          <h1>Create a new client token</h1>
          <div>
            <p>Add a client token to monitor who's accessing the API with your organization credentials.</p>
              <form action="/portal/organizations/#{org.path_id}/client_tokens" accept-charset="UTF-8" method="post">
                <div class="margin-bottom-4">
                  <label class="usa-label" for="label">Enter token name</label>
                  <span id="label_hint" class="text-base-darker">Choose a name that will be easy to identify.</span>
                  <input type="text" name="label" id="label" maxlength="25" class="usa-input" aria-describedby="label_hint" />
                </div>
                <input type="submit" name="commit" value="Generate token" class="usa-button" data-test="form-submit" data-disable-with="Generate token" />
              <p class="margin-top-5"><a href="https://dpc.cms.gov/docsV1">View API Documentation</a></p>
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
