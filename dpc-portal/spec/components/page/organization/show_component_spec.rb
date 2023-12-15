# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Page::Organization::ShowComponent, type: :component do
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
      let(:org) { MockOrg.new(0) }
      it 'Should have org name' do
        is_expected.to include("<h1>#{org.name}</h1>")
      end
      it 'Should have npi' do
        is_expected.to include("<div><strong>NPI</strong> #{org.npi}</div>")
      end
      it 'Should have Generate token button' do
        button = <<~BUTTON
          <form class="button_to" method="get" action="/portal/">
            <button class="usa-button" type="submit">Generate token</button>
          </form>
        BUTTON
        is_expected.to include(normalize_space(button))
      end
      it 'Should have Create key button' do
        button = <<~BUTTON
          <form class="button_to" method="get" action="/portal/">
            <button class="usa-button" type="submit">Create key</button>
          </form>
        BUTTON
        is_expected.to include(normalize_space(button))
      end
      it 'Should have Add IP button' do
        button = <<~BUTTON
          <form class="button_to" method="get" action="/portal/">
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
      let(:org) { MockOrg.new(2) }
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
        row = <<~HTML
          <tbody>
            <tr>
              <td data-sort-value="Token 1">Token 1</td>
              <td data-sort-value="12/16/2023 at  5:01PM UTC">12/16/2023 at  5:01PM UTC</td>
              <td data-sort-value="12/15/2023 at  5:01PM UTC">12/15/2023 at  5:01PM UTC</td>
              <td data-sort-value="X">X</td>
            </tr>
            <tr>
              <td data-sort-value="Token 2">Token 2</td>
              <td data-sort-value="12/16/2023 at  5:01PM UTC">12/16/2023 at  5:01PM UTC</td>
              <td data-sort-value="12/15/2023 at  5:01PM UTC">12/15/2023 at  5:01PM UTC</td>
              <td data-sort-value="X">X</td>
            </tr>
          </tbody>
        HTML
        is_expected.to include(normalize_space(row))
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
        row = <<~HTML
          <tbody>
            <tr>
              <td data-sort-value="Key 1">Key 1</td>
              <td data-sort-value="99790463-de1f-4f7f-a529-3e4f59dc7130">99790463-de1f-4f7f-a529-3e4f59dc7130</td>
              <td data-sort-value="12/15/2023 at  5:01PM UTC">12/15/2023 at  5:01PM UTC</td>
              <td data-sort-value="X">X</td>
            </tr>
            <tr>
              <td data-sort-value="Key 2">Key 2</td>
              <td data-sort-value="99790463-de1f-4f7f-a529-3e4f59dc7131">99790463-de1f-4f7f-a529-3e4f59dc7131</td>
              <td data-sort-value="12/15/2023 at  5:01PM UTC">12/15/2023 at  5:01PM UTC</td>
              <td data-sort-value="X">X</td>
            </tr>
          </tbody>
        HTML
        is_expected.to include(normalize_space(row))
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
        row = <<~HTML
          <tbody>
            <tr>
              <td data-sort-value="IP Addr 1">IP Addr 1</td>
              <td data-sort-value="127.0.0.10">127.0.0.10</td>
              <td data-sort-value="12/15/2023 at  5:01PM UTC">12/15/2023 at  5:01PM UTC</td>
              <td data-sort-value="X">X</td>
            </tr>
            <tr>
              <td data-sort-value="IP Addr 2">IP Addr 2</td>
              <td data-sort-value="127.0.0.11">127.0.0.11</td>
              <td data-sort-value="12/15/2023 at  5:01PM UTC">12/15/2023 at  5:01PM UTC</td>
              <td data-sort-value="X">X</td>
            </tr>
          </tbody>
        HTML
        is_expected.to include(normalize_space(row))
      end
    end
  end

  def normalize_space(str)
    str.gsub(/^ +/, '').gsub("\n", '')
  end
end

# Mocks the Organization class
class MockOrg
  attr_accessor :name, :npi

  def initialize(row_count)
    @name = 'Health'
    @npi = '11111'
    @row_count = row_count
    @created = '2023-12-15 17:01'
    @expires = '2023-12-16 17:01'
    @guid = '99790463-de1f-4f7f-a529-3e4f59dc713'
  end

  def client_tokens
    tokens = []
    @row_count.times do |index|
      tokens << { 'label' => "Token #{index + 1}",
                  'expiresAt' => @expires,
                  'createdAt' => @created }
    end
    tokens
  end

  def public_keys
    tokens = []
    @row_count.times do |index|
      tokens << { 'label' => "Key #{index + 1}",
                  'id' => @guid + index.to_s,
                  'createdAt' => @created }
    end
    tokens
  end

  def public_ips
    tokens = []
    @row_count.times do |index|
      tokens << { 'label' => "IP Addr #{index + 1}",
                  'ip_addr' => "127.0.0.#{index + 10}",
                  'createdAt' => @created }
    end
    tokens
  end
end
