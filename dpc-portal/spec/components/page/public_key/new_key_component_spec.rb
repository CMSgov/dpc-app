# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::PublicKey::NewKeyComponent, type: :component do
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
          <h1>Add Public Key</h1>
          <div>
            <h2>New Public Key for #{org.name}</h2>
            <hr/>
            <form action="/portal/organizations/#{org.path_id}/public_keys" accept-charset="UTF-8" method="post">
              <div class="margin-bottom-4">
                <label class="usa-label" for="label">Label</label>
                <p class="usa-hint">Choose a descriptive name to make your key easily identifiable to you.</p>
                <input type="text" name="label" id="label" max_length="25" class="usa-input" />
              </div>
              <div class="margin-bottom-4">
                <label class="usa-public-key" for="public-key">Public Key</label>
                <p class="usa-hint">Must include the "BEGIN PUBLIC KEY" and "END PUBLIC KEY" tags from your public.pem file.</p>
                <input type="textarea" name="public-key" id="public-key" class="usa-public-key" />
              </div>
              <div class="margin-bottom-4">
                <label class="usa-signature-snippet" for="signature-snippet">Signature Snippet</label>
                <p class="usa-hint">Must yield "Verified Ok" results in order to generate the signature.sig file.</p>
                <input type="textarea" name="signature-snippet" id="signature-snippet" class="usa-signature-snippet" />
              </div>
              <input type="submit" name="commit" value="Create key" class="usa-button" data-test="form-submit" data-disable-with="Create key" />
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
