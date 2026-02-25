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

    context 'no errors' do
      let(:component) { described_class.new(org, errors: {}) }
      let(:expected_html) do
        <<~HTML
          <div>
            <div class="margin-bottom-5">← <a href="/portal/organizations/#{org.path_id}">Back to organization</a></div>
            <h1>Add public IP address</h1>
            <section class="box">
              <div>
                <p>Provide a maximum of 8 public IP addresses associated with systems that will access claims data.</p>
                <form action="/portal/organizations/#{org.path_id}/ip_addresses" accept-charset="UTF-8" method="post">
                  <div class="margin-bottom-4">
                    <label class="usa-label" for="ip_address">Public IP address</label>
                    <span id="ip_address_hint" class="text-base-darker">Enter your IP address in the form XXX.XXX.XX.XX. Only IPv4 addresses are allowed. IP address ranges are not supported.</span>
                    <input type="text" name="ip_address" id="ip_address" maxlength="15" class="usa-input" aria-describedby="ip_address_hint" />
                  </div>
                  <input type="submit" name="commit" value="Add IP address" class="usa-button" data-test="form:submit" data-disable-with="Add IP address" />
                </form>
                <p class="margin-top-5"><a href="https://dpc.cms.gov/docsV1">View API Documentation</a></p>
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
    context 'with error on' do
      let(:component) { described_class.new(org, errors:) }
      context 'public key' do
        let(:errors) { { ip_address: 'Bad IP Address' } }
        it 'should show error' do
          bad_ip_address = <<~HTML
            <span id="ip_address_error_msg" class="usa-error-message" role="alert">Bad IP Address</span>
            <input type="text" name="ip_address" id="ip_address" maxlength="15" class="usa-input usa-input--error" aria-describedby="ip_address_hint" />
          HTML
          is_expected.to include(normalize_space(bad_ip_address))
        end
      end
    end
  end
end
