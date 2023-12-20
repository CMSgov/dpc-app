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
          <div class="margin-bottom-5">← <a href="/portal/organizations/#{org.path_id}">#{org.name}</a></div>
          <h1>Create a new client token</h1>
          <div>
            <h2>New token for #{org.name}</h2>
            <hr/>
            <form action="/portal/organizations/#{org.path_id}/client_tokens" accept-charset="UTF-8" method="post">
              <div class="margin-bottom-4">
                <label class="usa-label" for="label">Label</label>
                <p class="usa-hint">Choose a descriptive name to make your token easily identifiable to you.</p>
                <input type="text" name="label" id="label" max_length="25" class="usa-input" />
              </div>
              <input type="submit" name="commit" value="Create token" class="usa-button" data-test="form-submit" data-disable-with="Create token" />
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
