# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::IpAddress::NewAddressComponent, type: :component do
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
          <h1>Add Public IP Address</h1>
          <section class="box">
            <div>
              <h2>New IP address for #{org.name}</h2>
              <form action="/portal/organizations/#{org.path_id}/ip_addresses" accept-charset="UTF-8" method="post">
                <div class="margin-bottom-4">
                  <label class="usa-label" for="label">Label</label>
                  <p class="usa-hint">Choose a descriptive name to make your IP address easily identifiable to you.</p>
                  <input type="text" name="label" id="label" maxlength="25" class="usa-input">
                </div>
                <div class="margin-bottom-4">
                  <label class="usa-label" for="ip_address">Public IP address</label>
                  <p class="usa-hint">For example, 136.226.19.87</p>
                  <input type="text" name="ip_address" id="ip_address" maxlength="15" class="usa-input">
                </div>
                <input type="submit" name="commit" value="Add IP" class="usa-button" data-test="form:submit" data-disable-with="Add IP">
              </form>
            </div>
          </section>
        </div>
      HTML
    end
    before do
      render_inline(component)
    end

    it { is_expected.to match_html_fragment(expected_html) }
  end
end
