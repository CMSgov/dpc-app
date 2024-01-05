# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Organization::ShowComponent, type: :component do
  include ComponentSupport
  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end

    let(:component) { described_class.new(org) }

    before do
      render_inline(component)
    end

    context 'No tokens, keys, or ip addrs' do
      let(:org) { ComponentSupport::MockOrg.new(0) }
      it 'Should have org name' do
        is_expected.to include("<h1>#{org.name}</h1>")
      end
      it 'Should have npi' do
        is_expected.to include("<div><strong>NPI</strong> #{org.npi}</div>")
      end
      it 'Should have Generate token button' do
        button = <<~BUTTON
          <form class="button_to" method="get" action="/portal/organizations/#{org.path_id}/client_tokens/new">
            <button class="usa-button" type="submit">Generate token</button>
          </form>
        BUTTON
        is_expected.to include(normalize_space(button))
      end
      it 'Should have Create key button' do
        button = <<~BUTTON
          <form class="button_to" method="get" action="/portal/organizations/#{org.path_id}/public_keys/new">
            <button class="usa-button" type="submit">Create key</button>
          </form>
        BUTTON
        is_expected.to include(normalize_space(button))
      end
      it 'Should have Add IP button' do
        button = <<~BUTTON
          <form class="button_to" method="get" action="/portal/organizations/#{org.path_id}/ip_addresses/new">
            <button class="usa-button" type="submit">Add IP</button>
          </form>
        BUTTON
        is_expected.to include(normalize_space(button))
      end
      it 'Should have no tokens message' do
        no_token = ['<p>Before you can access production data,',
                    'you must create a unique client token for',
                    'each application or vendor that will have access to API.</p>']
        is_expected.to include(no_token.join(' '))
      end
      it 'Should have no keys message' do
        no_key = ['<p>Before you can access production data,',
                  'add your public keys to get a UUID that you',
                  'will use when you authenticate access.</p>']
        is_expected.to include(no_key.join(' '))
      end
      it 'Should have no ip_addrs message' do
        no_ip_addr = '<p>Before you can access production data, you must provide a public IP address (max of 8).</p>'
        is_expected.to include(no_ip_addr)
      end
    end
    context 'Tokens, keys, and ip addrs' do
      let(:org) { ComponentSupport::MockOrg.new(2) }
      it 'should have token table header' do
        header = <<~HTML
          <caption aria-hidden="true" hidden>Public Key Table</caption>
          <thead>
            <tr>
              <th scope="row" role="columnheader">Label</th>
              <th scope="row" role="columnheader">Key ID</th>
              <th data-sortable scope="row" role="columnheader" aria-sort="ascending">
                Creation Date
              </th>
              <th scope="row" role="columnheader"></th>
            </tr>
          </thead>
        HTML
        is_expected.to include(normalize_space(header))
      end
      it 'should have token rows' do
        row1 = <<~HTML
          <tr>
            <td data-sort-value="Token 1">Token 1</td>
            <td data-sort-value="12/16/2023 at  5:01PM UTC">12/16/2023 at  5:01PM UTC</td>
            <td data-sort-value="12/15/2023 at  5:01PM UTC">12/15/2023 at  5:01PM UTC</td>
        HTML
        row2 = <<~HTML
          <tr>
            <td data-sort-value="Token 2">Token 2</td>
            <td data-sort-value="12/16/2023 at  5:01PM UTC">12/16/2023 at  5:01PM UTC</td>
            <td data-sort-value="12/15/2023 at  5:01PM UTC">12/15/2023 at  5:01PM UTC</td>
        HTML
        is_expected.to include(normalize_space(row1))
        is_expected.to include(normalize_space(row2))
      end
      it 'should have delete token form' do
        form1 = <<~HTML
          <form class="button_to" method="post" action="/portal/organizations/99790463-de1f-4f7f-a529-3e4f59dc7131/client_tokens/token-id-1">
           <input type="hidden" name="_method" value="delete" autocomplete="off" />
           <button class="usa-button" type="submit">Yes, revoke token</button>
          </form>
        HTML
        form2 = <<~HTML
          <form class="button_to" method="post" action="/portal/organizations/99790463-de1f-4f7f-a529-3e4f59dc7131/client_tokens/token-id-2">
           <input type="hidden" name="_method" value="delete" autocomplete="off" />
           <button class="usa-button" type="submit">Yes, revoke token</button>
          </form>
        HTML
        is_expected.to include(normalize_space(form1))
        is_expected.to include(normalize_space(form2))
      end
      it 'should have key table header' do
        header = <<~HTML
          <caption aria-hidden="true" hidden>Public Key Table</caption>
          <thead>
            <tr>
              <th scope="row" role="columnheader">
                Label
              </th>
              <th scope="row" role="columnheader">
                Key ID
              </th>
              <th data-sortable scope="row" role="columnheader" aria-sort="ascending">
                Creation Date
              </th>
              <th scope="row" role="columnheader">
              </th>
            </tr>
          </thead>
        HTML
        is_expected.to include(normalize_space(header))
      end
      it 'should have key rows' do
        row1 = <<~HTML
          <tr>
            <td data-sort-value="Key 1">Key 1</td>
            <td data-sort-value="99790463-de1f-4f7f-a529-3e4f59dc7130">99790463-de1f-4f7f-a529-3e4f59dc7130</td>
            <td data-sort-value="12/15/2023 at  5:01PM UTC">12/15/2023 at  5:01PM UTC</td>
        HTML
        row2 = <<~HTML
          <tr>
            <td data-sort-value="Key 2">Key 2</td>
            <td data-sort-value="99790463-de1f-4f7f-a529-3e4f59dc7131">99790463-de1f-4f7f-a529-3e4f59dc7131</td>
            <td data-sort-value="12/15/2023 at  5:01PM UTC">12/15/2023 at  5:01PM UTC</td>
        HTML
        is_expected.to include(normalize_space(row1))
        is_expected.to include(normalize_space(row2))
      end
      it 'should have delete key form' do
        form1 = <<~HTML
          <form class="button_to" method="post" action="/portal/organizations/99790463-de1f-4f7f-a529-3e4f59dc7131/public_keys/99790463-de1f-4f7f-a529-3e4f59dc7130">
            <input type="hidden" name="_method" value="delete" autocomplete="off" />
            <button class="usa-button" type="submit">Yes, revoke key</button>
          </form>
        HTML
        form2 = <<~HTML
          <form class="button_to" method="post" action="/portal/organizations/99790463-de1f-4f7f-a529-3e4f59dc7131/public_keys/99790463-de1f-4f7f-a529-3e4f59dc7131">
            <input type="hidden" name="_method" value="delete" autocomplete="off" />
            <button class="usa-button" type="submit">Yes, revoke key</button>
          </form>
        HTML
        is_expected.to include(normalize_space(form1))
        is_expected.to include(normalize_space(form2))
      end
      it 'should have ip_addr table header' do
        header = <<~HTML
          <caption aria-hidden="true" hidden>Public IP Table</caption>
          <thead>
            <tr>
              <th scope="row" role="columnheader">
                Label
              </th>
              <th scope="row" role="columnheader">
                Public IP
              </th>
              <th data-sortable scope="row" role="columnheader" aria-sort="ascending">
                Date Added
              </th>
              <th scope="row" role="columnheader">
              </th>
            </tr>
          </thead>
        HTML
        is_expected.to include(normalize_space(header))
      end
      it 'should have ip_addr row' do
        row1 = <<~HTML
          <tr>
              <td data-sort-value="IP Addr 1">IP Addr 1</td>
              <td data-sort-value="127.0.0.10">127.0.0.10</td>
              <td data-sort-value="12/15/2023 at  5:01PM UTC">12/15/2023 at  5:01PM UTC</td>
        HTML
        row2 = <<~HTML
          <tr>
            <td data-sort-value="IP Addr 2">IP Addr 2</td>
            <td data-sort-value="127.0.0.11">127.0.0.11</td>
            <td data-sort-value="12/15/2023 at  5:01PM UTC">12/15/2023 at  5:01PM UTC</td>
        HTML
        is_expected.to include(normalize_space(row1))
        is_expected.to include(normalize_space(row2))
      end
      it 'should have delete address form' do
        form1 = <<~HTML
          <form class="button_to" method="post" action="/portal/organizations/99790463-de1f-4f7f-a529-3e4f59dc7131/ip_addresses/">
            <input type="hidden" name="_method" value="delete" autocomplete="off" />
            <button class="usa-button" type="submit">Yes, revoke address</button>
          </form>
        HTML
        form2 = <<~HTML
          <form class="button_to" method="post" action="/portal/organizations/99790463-de1f-4f7f-a529-3e4f59dc7131/ip_addresses/">
            <input type="hidden" name="_method" value="delete" autocomplete="off" />
            <button class="usa-button" type="submit">Yes, revoke address</button>
          </form>
        HTML
        is_expected.to include(normalize_space(form1))
        is_expected.to include(normalize_space(form2))
      end
    end
  end
end
