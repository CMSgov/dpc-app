# frozen_string_literal: true

require 'rails_helper'
RSpec.describe CspUtils do
  let(:idp_host) { 'idp.example.com' }
  let(:issuer) { "https://#{idp_host}/oidc" }
  before do
    stub_const('CspUtils::CODES_TO_CONFIGS',
               { id_me: { name: :id_me, issuer: issuer, client_options: { host: idp_host } } })
  end

  describe '.issuer' do
    it 'returns the issuer for a known host' do
      expect(described_class.issuer(idp_host)).to eq(issuer)
    end

    it 'raises an error for an unknown host' do
      expect { described_class.issuer('unknown-host') }.to raise_error(ArgumentError, /Unknown CSP host/)
    end
  end
end
